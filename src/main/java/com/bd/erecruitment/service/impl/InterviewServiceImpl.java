package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.InterviewFeedbackReqDto;
import com.bd.erecruitment.dto.req.ScheduleInterviewReqDto;
import com.bd.erecruitment.dto.res.InterviewResDTO;
import com.bd.erecruitment.entity.*;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.ApplicationStatusHistoryRepo;
import com.bd.erecruitment.repository.InterviewRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
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

@Slf4j
@Service
public class InterviewServiceImpl extends AbstractBaseService<Interview> {

	// Same pragmatic "is staff" proxy as ApplicationServiceImpl.
	private static final String STAFF_AUTHORITY = "job-circular:write";

	private final InterviewRepo interviewRepo;
	private final ApplicationRepo applicationRepo;
	private final ApplicationStatusHistoryRepo historyRepo;
	private final JobCircularRepo jobCircularRepo;
	private final UserRepo userRepo;
	private final MailService mailService;

	public InterviewServiceImpl(InterviewRepo interviewRepo, ApplicationRepo applicationRepo,
			ApplicationStatusHistoryRepo historyRepo, JobCircularRepo jobCircularRepo, UserRepo userRepo,
			MailService mailService) {
		super(interviewRepo);
		this.interviewRepo = interviewRepo;
		this.applicationRepo = applicationRepo;
		this.historyRepo = historyRepo;
		this.jobCircularRepo = jobCircularRepo;
		this.userRepo = userRepo;
		this.mailService = mailService;
	}

	@Transactional
	public Response<InterviewResDTO> schedule(ScheduleInterviewReqDto reqDto) {
		requireStaff("schedule interviews");
		if (reqDto.getApplicationId() == null) returnErrorException("Application is required");
		if (StringUtils.isBlank(reqDto.getTitle())) returnErrorException("Interview title is required");
		if (reqDto.getScheduledAt() == null) returnErrorException("Scheduled time is required");

		Application application = applicationRepo.findByIdAndDeleted(reqDto.getApplicationId(), false)
			.orElseThrow(() -> new NotFoundException("Application not found"));

		Interview interview = new Interview()
			.setApplicationId(application.getId())
			.setTitle(reqDto.getTitle())
			.setScheduledAt(reqDto.getScheduledAt())
			.setDurationMinutes(reqDto.getDurationMinutes())
			.setMode(reqDto.getMode())
			.setLocation(reqDto.getLocation())
			.setStatus("SCHEDULED");
		if (reqDto.getInterviewerUserIds() != null) interview.getInterviewerUserIds().addAll(reqDto.getInterviewerUserIds());

		interview = createEntity(interview);
		notifyScheduled(interview, application);

		// Recorded into ApplicationStatusHistory too (not just the Application row) so the pipeline
		// trail and time-to-hire/funnel analytics see "moved to interview" alongside manual status changes.
		if (!"INTERVIEW".equals(application.getStatus())) {
			String actor = getLoggedInUserDetails().getUsername();
			application.setStatus("INTERVIEW").setStatusUpdatedOn(new Date()).setStatusUpdatedBy(actor);
			updateEntityApplication(application);
			recordHistory(application.getId(), "INTERVIEW", "Interview scheduled: " + interview.getTitle(), actor);
		}

		return getCreatedResponse("Interview scheduled successfully", toDto(interview, null, null));
	}

	// Interview entities are managed here, but Application's audit/updatedBy bookkeeping still
	// needs AbstractBaseService's own instance state (auditSnapshots) - simplest to just persist
	// directly here rather than re-deriving an AbstractBaseService<Application>.
	private void updateEntityApplication(Application application) {
		applicationRepo.save(application);
	}

	public Response<InterviewResDTO> find(Long id) {
		Interview interview = getOwnedOrStaffInterview(id);
		return getSuccessResponse("Interview found", toDto(interview, null, null));
	}

	public Response<InterviewResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		requireStaff("view interviews");
		Specification<Interview> spec = com.bd.erecruitment.specification.GenericSpecification.build(filters);
		if (Boolean.TRUE.equals(isPageable)) {
			Page<Interview> page = interviewRepo.findAll(spec, pageable);
			return getSuccessResponse(page.hasContent() ? "Found" : "No data found", page.map(i -> toDto(i, null, null)));
		}
		List<InterviewResDTO> list = interviewRepo.findAll(spec).stream().map(i -> toDto(i, null, null)).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	public Response<InterviewResDTO> findByApplication(Long applicationId) {
		getOwnedOrStaffApplication(applicationId);
		List<InterviewResDTO> list = interviewRepo.findAllByApplicationIdAndDeletedOrderByScheduledAtAsc(applicationId, false)
			.stream().map(i -> toDto(i, null, null)).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	@Transactional
	public Response<InterviewResDTO> changeStatus(Long id, String status) {
		requireStaff("update interviews");
		Interview interview = findByIdOrThrow(id, "Interview not found");
		interview.setStatus(status);
		interview = updateEntity(interview);
		return getSuccessResponse("Interview updated successfully", toDto(interview, null, null));
	}

	@Transactional
	public Response<InterviewResDTO> submitFeedback(Long id, InterviewFeedbackReqDto reqDto) {
		Interview interview = findByIdOrThrow(id, "Interview not found");
		MyUserDetail me = getLoggedInUserDetails();
		if (!interview.getInterviewerUserIds().contains(me.getId()) && !isStaff(me)) {
			throw new ForbiddenException("Only assigned interviewers may submit feedback");
		}

		User user = userRepo.findByIdAndDeleted(me.getId(), false).orElse(null);
		InterviewFeedbackItem item = new InterviewFeedbackItem()
			.setInterviewerUserId(me.getId())
			.setInterviewerName(user != null ? user.getFullName() : me.getUsername())
			.setRating(reqDto.getRating())
			.setComments(reqDto.getComments())
			.setSubmittedOn(new Date());

		interview.getFeedback().removeIf(f -> f.getInterviewerUserId().equals(me.getId()));
		interview.getFeedback().add(item);
		interview = updateEntity(interview);

		return getSuccessResponse("Feedback submitted successfully", toDto(interview, null, null));
	}

	private void recordHistory(Long applicationId, String status, String note, String actor) {
		Date now = new Date();
		ApplicationStatusHistory history = new ApplicationStatusHistory()
			.setApplicationId(applicationId)
			.setStatus(status)
			.setNote(note)
			.setChangedBy(actor)
			.setChangedOn(now);
		history.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		historyRepo.save(history);
	}

	private Interview getOwnedOrStaffInterview(Long id) {
		Interview interview = findByIdOrThrow(id, "Interview not found");
		getOwnedOrStaffApplication(interview.getApplicationId());
		return interview;
	}

	private Application getOwnedOrStaffApplication(Long applicationId) {
		Application application = applicationRepo.findByIdAndDeleted(applicationId, false)
			.orElseThrow(() -> new NotFoundException("Application not found"));
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

	private InterviewResDTO toDto(Interview interview, JobCircular jobHint, User candidateHint) {
		InterviewResDTO dto = new InterviewResDTO(interview);
		Application application = applicationRepo.findByIdAndDeleted(interview.getApplicationId(), false).orElse(null);
		if (application != null) {
			JobCircular job = jobHint != null ? jobHint : jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			if (job != null) dto.setJobTitle(job.getJobTitle());
			User candidate = candidateHint != null ? candidateHint : userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			if (candidate != null) dto.setCandidateName(candidate.getFullName());
		}
		return dto;
	}

	private void notifyScheduled(Interview interview, Application application) {
		try {
			User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			if (candidate == null || job == null) return;
			mailService.sendInterviewScheduledEmail(candidate.getEmail(), candidate.getFullName(), job.getJobTitle(),
				interview.getTitle(), interview.getScheduledAt(), interview.getMode(), interview.getLocation());
		} catch (Exception e) {
			log.warn("Failed to send interview-scheduled email for interview {}: {}", interview.getId(), e.getMessage());
		}
	}
}
