package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.ApplicationStatusChangeReqDto;
import com.bd.erecruitment.dto.req.ApplyReqDto;
import com.bd.erecruitment.dto.res.ApplicationResDTO;
import com.bd.erecruitment.dto.res.ApplicationStatusHistoryResDTO;
import com.bd.erecruitment.entity.*;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.*;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.StorageService;
import com.bd.erecruitment.specification.GenericSpecification;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class ApplicationServiceImpl extends AbstractBaseService<Application> {

	// Staff (recruiter/admin) actions beyond a candidate's own applications are gated on this
	// authority as a pragmatic stand-in for "has recruiting/admin access" - PermissionInterceptor
	// only enforces one resource:action pair per whole controller, so distinguishing "my
	// applications" (any candidate) from "all applications" (staff only) under the same
	// "application" resource has to happen here, not at the interceptor.
	private static final String STAFF_AUTHORITY = "job-circular:write";
	private static final Set<String> VALID_STATUSES = Set.of(
		"APPLIED", "SCREENING", "INTERVIEW", "OFFER", "HIRED", "REJECTED", "WITHDRAWN"
	);

	private final ApplicationRepo applicationRepo;
	private final ApplicationStatusHistoryRepo historyRepo;
	private final JobCircularRepo jobCircularRepo;
	private final UserRepo userRepo;
	private final CandidateProfileRepo candidateProfileRepo;
	private final GeneratedCvRepo generatedCvRepo;
	private final StorageService storageService;
	private final MailService mailService;

	public ApplicationServiceImpl(ApplicationRepo applicationRepo, ApplicationStatusHistoryRepo historyRepo,
			JobCircularRepo jobCircularRepo, UserRepo userRepo, CandidateProfileRepo candidateProfileRepo,
			GeneratedCvRepo generatedCvRepo, StorageService storageService, MailService mailService) {
		super(applicationRepo);
		this.applicationRepo = applicationRepo;
		this.historyRepo = historyRepo;
		this.jobCircularRepo = jobCircularRepo;
		this.userRepo = userRepo;
		this.candidateProfileRepo = candidateProfileRepo;
		this.generatedCvRepo = generatedCvRepo;
		this.storageService = storageService;
		this.mailService = mailService;
	}

	@Transactional
	public Response<ApplicationResDTO> apply(ApplyReqDto reqDto) {
		if (reqDto.getJobCircularId() == null) returnErrorException("Job is required");
		JobCircular job = jobCircularRepo.findByIdAndDeleted(reqDto.getJobCircularId(), false)
			.orElseThrow(() -> new NotFoundException("Job not found"));
		if (!"PUBLISHED".equals(job.getStatus())) returnErrorException("This job is not open for applications");

		MyUserDetail me = getLoggedInUserDetails();
		if (applicationRepo.findByJobCircularIdAndCandidateUserIdAndDeleted(job.getId(), me.getId(), false).isPresent()) {
			returnErrorException("You have already applied to this job");
		}

		Long generatedCvId = null;
		if (!Boolean.FALSE.equals(reqDto.getUseLatestGeneratedCv())) {
			List<GeneratedCv> cvs = generatedCvRepo.findAllByCandidateProfileIdAndDeletedOrderByGeneratedOnDesc(
				candidateProfileIdFor(me.getId()), false);
			if (!cvs.isEmpty()) generatedCvId = cvs.get(0).getId();
		}
		if (generatedCvId == null && reqDto.getResumeFileId() == null) {
			returnErrorException("Generate a CV from your profile or attach a resume before applying");
		}

		Application application = new Application()
			.setJobCircularId(job.getId())
			.setCandidateUserId(me.getId())
			.setStatus("APPLIED")
			.setCoverLetter(reqDto.getCoverLetter())
			.setResumeFileId(reqDto.getResumeFileId())
			.setGeneratedCvId(generatedCvId)
			.setAppliedOn(new Date())
			.setStatusUpdatedOn(new Date())
			.setStatusUpdatedBy(me.getUsername());

		application = createEntity(application);
		recordHistory(application.getId(), "APPLIED", "Application submitted", me.getUsername());

		notifyApplicationReceived(me, job);
		notifyNewApplication(job, me);

		return getSuccessResponse("Application submitted successfully", toDto(application, job, null));
	}

	public Response<ApplicationResDTO> getMyApplications() {
		Long userId = getLoggedInUserDetails().getId();
		List<ApplicationResDTO> list = applicationRepo
			.findAllByCandidateUserIdAndDeletedOrderByAppliedOnDesc(userId, false)
			.stream().map(a -> toDto(a, null, null)).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	public Response<ApplicationResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		requireStaff("view all applications");
		Specification<Application> spec = GenericSpecification.build(filters);
		if (Boolean.TRUE.equals(isPageable)) {
			Page<Application> page = applicationRepo.findAll(spec, pageable);
			return getSuccessResponse(page.hasContent() ? "Found" : "No data found", page.map(a -> toDto(a, null, null)));
		}
		List<ApplicationResDTO> list = applicationRepo.findAll(spec).stream().map(a -> toDto(a, null, null)).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	public Response<ApplicationResDTO> find(Long id) {
		Application application = getOwnedOrStaffApplication(id);
		return getSuccessResponse("Application found", toDto(application, null, null));
	}

	@Transactional
	public Response<ApplicationResDTO> changeStatus(Long id, ApplicationStatusChangeReqDto reqDto) {
		requireStaff("change an application's status");
		if (StringUtils.isBlank(reqDto.getStatus()) || !VALID_STATUSES.contains(reqDto.getStatus())) {
			returnErrorException("A valid status is required");
		}
		Application application = findByIdOrThrow(id, "Application not found");
		MyUserDetail me = getLoggedInUserDetails();

		application.setStatus(reqDto.getStatus())
			.setStatusUpdatedOn(new Date())
			.setStatusUpdatedBy(me.getUsername());
		application = updateEntity(application);
		recordHistory(application.getId(), reqDto.getStatus(), reqDto.getNote(), me.getUsername());

		notifyStatusChanged(application);
		return getSuccessResponse("Application status updated successfully", toDto(application, null, null));
	}

	public Response<ApplicationStatusHistoryResDTO> getHistory(Long id) {
		getOwnedOrStaffApplication(id);
		List<ApplicationStatusHistoryResDTO> list = historyRepo
			.findAllByApplicationIdAndDeletedOrderByChangedOnAsc(id, false)
			.stream().map(ApplicationStatusHistoryResDTO::new).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	public StoredFile downloadCv(Long applicationId) {
		Application application = getOwnedOrStaffApplication(applicationId);
		if (application.getGeneratedCvId() == null) throw new NotFoundException("No generated CV on this application");
		GeneratedCv cv = generatedCvRepo.findByIdAndDeleted(application.getGeneratedCvId(), false)
			.orElseThrow(() -> new NotFoundException("CV not found"));
		return storageService.retrieve(cv.getStoredFileId());
	}

	private Application getOwnedOrStaffApplication(Long id) {
		Application application = findByIdOrThrow(id, "Application not found");
		MyUserDetail me = getLoggedInUserDetails();
		if (!application.getCandidateUserId().equals(me.getId()) && !isStaff(me)) {
			throw new ForbiddenException("Access denied");
		}
		return application;
	}

	private void requireStaff(String action) {
		if (!isStaff(getLoggedInUserDetails())) {
			throw new ForbiddenException("Only recruiters/admins may " + action);
		}
	}

	private boolean isStaff(MyUserDetail user) {
		return user.getAuthorities().stream().anyMatch(a ->
			STAFF_AUTHORITY.equals(a.getAuthority()) || "SUPER_ADMIN".equals(a.getAuthority()));
	}

	private void recordHistory(Long applicationId, String status, String note, String actor) {
		ApplicationStatusHistory history = new ApplicationStatusHistory()
			.setApplicationId(applicationId)
			.setStatus(status)
			.setNote(note)
			.setChangedBy(actor)
			.setChangedOn(new Date());
		Date now = new Date();
		history.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		historyRepo.save(history);
	}

	private Long candidateProfileIdFor(Long userId) {
		return candidateProfileRepo.findByUserIdAndDeleted(userId, false)
			.map(CandidateProfile::getId)
			.orElse(null);
	}

	private ApplicationResDTO toDto(Application application, JobCircular jobHint, User candidateHint) {
		ApplicationResDTO dto = new ApplicationResDTO(application);
		JobCircular job = jobHint != null ? jobHint
			: jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
		if (job != null) dto.setJobTitle(job.getJobTitle());

		User candidate = candidateHint != null ? candidateHint
			: userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
		if (candidate != null) {
			dto.setCandidateName(candidate.getFullName());
			dto.setCandidateEmail(candidate.getEmail());
		}
		return dto;
	}

	private void notifyApplicationReceived(MyUserDetail candidate, JobCircular job) {
		try {
			mailService.sendApplicationReceivedEmail(candidate.getUsername(), null, job.getJobTitle());
		} catch (Exception e) {
			log.warn("Failed to send application-received email for job {}: {}", job.getId(), e.getMessage());
		}
	}

	private void notifyNewApplication(JobCircular job, MyUserDetail candidate) {
		if (StringUtils.isBlank(job.getCreatedBy())) return;
		try {
			User candidateEntity = userRepo.findByIdAndDeleted(candidate.getId(), false).orElse(null);
			String candidateName = candidateEntity != null ? candidateEntity.getFullName() : candidate.getUsername();
			mailService.sendNewApplicationEmail(job.getCreatedBy(), job.getJobTitle(), candidateName);
		} catch (Exception e) {
			log.warn("Failed to send new-application email for job {}: {}", job.getId(), e.getMessage());
		}
	}

	private void notifyStatusChanged(Application application) {
		try {
			User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			if (candidate == null || job == null) return;
			mailService.sendApplicationStatusChangedEmail(candidate.getEmail(), candidate.getFullName(),
				job.getJobTitle(), application.getStatus(), null);
		} catch (Exception e) {
			log.warn("Failed to send status-changed email for application {}: {}", application.getId(), e.getMessage());
		}
	}
}
