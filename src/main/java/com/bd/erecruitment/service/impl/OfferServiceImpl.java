package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.CreateOfferReqDto;
import com.bd.erecruitment.dto.req.OfferResponseReqDto;
import com.bd.erecruitment.dto.res.OfferResDTO;
import com.bd.erecruitment.entity.*;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.ApplicationStatusHistoryRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.OfferRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.StorageService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class OfferServiceImpl extends AbstractBaseService<Offer> {

	private static final String STAFF_AUTHORITY = "job-circular:write";
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy");

	private final OfferRepo offerRepo;
	private final ApplicationRepo applicationRepo;
	private final ApplicationStatusHistoryRepo historyRepo;
	private final JobCircularRepo jobCircularRepo;
	private final UserRepo userRepo;
	private final StorageService storageService;
	private final HtmlToPdfRenderer pdfRenderer;
	private final MailService mailService;
	private final OnboardingServiceImpl onboardingService;

	public OfferServiceImpl(OfferRepo offerRepo, ApplicationRepo applicationRepo, ApplicationStatusHistoryRepo historyRepo,
			JobCircularRepo jobCircularRepo, UserRepo userRepo, StorageService storageService, HtmlToPdfRenderer pdfRenderer,
			MailService mailService, OnboardingServiceImpl onboardingService) {
		super(offerRepo);
		this.offerRepo = offerRepo;
		this.applicationRepo = applicationRepo;
		this.historyRepo = historyRepo;
		this.jobCircularRepo = jobCircularRepo;
		this.userRepo = userRepo;
		this.storageService = storageService;
		this.pdfRenderer = pdfRenderer;
		this.mailService = mailService;
		this.onboardingService = onboardingService;
	}

	@Transactional
	public Response<OfferResDTO> createOffer(CreateOfferReqDto reqDto) {
		requireStaff("create offers");
		if (reqDto.getApplicationId() == null) returnErrorException("Application is required");
		Application application = applicationRepo.findByIdAndDeleted(reqDto.getApplicationId(), false)
			.orElseThrow(() -> new NotFoundException("Application not found"));

		Offer offer = new Offer()
			.setApplicationId(application.getId())
			.setPosition(reqDto.getPosition())
			.setSalaryOffered(reqDto.getSalaryOffered())
			.setStartDate(reqDto.getStartDate())
			.setExpiryDate(reqDto.getExpiryDate())
			.setNotes(reqDto.getNotes())
			.setStatus("DRAFT");
		offer = createEntity(offer);

		String actor = getLoggedInUserDetails().getUsername();
		application.setStatus("OFFER").setStatusUpdatedOn(new Date()).setStatusUpdatedBy(actor);
		applicationRepo.save(application);
		recordHistory(application.getId(), "OFFER", "Offer created", actor);

		return getCreatedResponse("Offer created successfully", toDto(offer, null, null));
	}

	@Transactional
	public Response<OfferResDTO> generateLetter(Long offerId) {
		requireStaff("generate offer letters");
		Offer offer = findByIdOrThrow(offerId, "Offer not found");
		Application application = applicationRepo.findByIdAndDeleted(offer.getApplicationId(), false)
			.orElseThrow(() -> new NotFoundException("Application not found"));
		JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false)
			.orElseThrow(() -> new NotFoundException("Job not found"));
		User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false)
			.orElseThrow(() -> new NotFoundException("Candidate not found"));

		String html = buildLetterHtml(offer, job, candidate);
		byte[] pdf = pdfRenderer.render(html);
		StoredFile stored = storageService.store("Offer-Letter-" + candidate.getFullName().replaceAll("\\s+", "_") + ".pdf", "application/pdf", pdf);

		offer.setOfferLetterFileId(stored.getId());
		offer = updateEntity(offer);

		return getSuccessResponse("Offer letter generated successfully", toDto(offer, job, candidate));
	}

	@Transactional
	public Response<OfferResDTO> send(Long offerId) {
		requireStaff("send offers");
		Offer offer = findByIdOrThrow(offerId, "Offer not found");
		if (offer.getOfferLetterFileId() == null) returnErrorException("Generate the offer letter before sending");

		offer.setStatus("SENT");
		offer = updateEntity(offer);

		Application application = applicationRepo.findByIdAndDeleted(offer.getApplicationId(), false).orElse(null);
		if (application != null) {
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			if (job != null && candidate != null) {
				try {
					mailService.sendOfferEmail(candidate.getEmail(), candidate.getFullName(), job.getJobTitle());
				} catch (Exception e) {
					log.warn("Failed to send offer email for offer {}: {}", offer.getId(), e.getMessage());
				}
			}
		}

		return getSuccessResponse("Offer sent successfully", toDto(offer, null, null));
	}

	public Response<OfferResDTO> find(Long id) {
		Offer offer = getOwnedOrStaffOffer(id);
		return getSuccessResponse("Offer found", toDto(offer, null, null));
	}

	public StoredFile downloadLetter(Long id) {
		Offer offer = getOwnedOrStaffOffer(id);
		if (offer.getOfferLetterFileId() == null) throw new NotFoundException("Offer letter not generated yet");
		return storageService.retrieve(offer.getOfferLetterFileId());
	}

	public Response<OfferResDTO> findByApplication(Long applicationId) {
		getOwnedOrStaffApplication(applicationId);
		List<OfferResDTO> list = offerRepo.findAllByApplicationIdAndDeletedOrderByIdDesc(applicationId, false)
			.stream().map(o -> toDto(o, null, null)).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	public Response<OfferResDTO> myOffers() {
		Long userId = getLoggedInUserDetails().getId();
		List<Application> myApplications = applicationRepo.findAllByCandidateUserIdAndDeletedOrderByAppliedOnDesc(userId, false);
		List<OfferResDTO> list = myApplications.stream()
			.flatMap(a -> offerRepo.findAllByApplicationIdAndDeletedOrderByIdDesc(a.getId(), false).stream())
			.map(o -> toDto(o, null, null))
			.toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	@Transactional
	public Response<OfferResDTO> respond(Long offerId, OfferResponseReqDto reqDto) {
		Offer offer = findByIdOrThrow(offerId, "Offer not found");
		Application application = applicationRepo.findByIdAndDeleted(offer.getApplicationId(), false)
			.orElseThrow(() -> new NotFoundException("Application not found"));
		MyUserDetail me = getLoggedInUserDetails();
		if (!application.getCandidateUserId().equals(me.getId())) {
			throw new ForbiddenException("Only the candidate may respond to this offer");
		}
		if (!"SENT".equals(offer.getStatus())) returnErrorException("This offer is not awaiting a response");

		offer.setStatus(reqDto.isAccept() ? "ACCEPTED" : "DECLINED").setRespondedOn(new Date());
		offer = updateEntity(offer);

		String newStatus = reqDto.isAccept() ? "HIRED" : "REJECTED";
		application.setStatus(newStatus).setStatusUpdatedOn(new Date()).setStatusUpdatedBy(me.getUsername());
		applicationRepo.save(application);
		recordHistory(application.getId(), newStatus, reqDto.isAccept() ? "Offer accepted" : "Offer declined", me.getUsername());

		if (reqDto.isAccept()) {
			onboardingService.seedDefaultTasks(application.getId(), me.getUsername());
		}

		notifyResponse(offer, application, reqDto.isAccept());

		return getSuccessResponse("Response recorded successfully", toDto(offer, null, null));
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

	private Offer getOwnedOrStaffOffer(Long id) {
		Offer offer = findByIdOrThrow(id, "Offer not found");
		getOwnedOrStaffApplication(offer.getApplicationId());
		return offer;
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

	private OfferResDTO toDto(Offer offer, JobCircular jobHint, User candidateHint) {
		OfferResDTO dto = new OfferResDTO(offer);
		Application application = applicationRepo.findByIdAndDeleted(offer.getApplicationId(), false).orElse(null);
		if (application != null) {
			JobCircular job = jobHint != null ? jobHint : jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			if (job != null) dto.setJobTitle(job.getJobTitle());
			User candidate = candidateHint != null ? candidateHint : userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			if (candidate != null) dto.setCandidateName(candidate.getFullName());
		}
		return dto;
	}

	private void notifyResponse(Offer offer, Application application, boolean accepted) {
		try {
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			if (job == null || candidate == null || StringUtils.isBlank(job.getCreatedBy())) return;
			mailService.sendOfferResponseEmail(job.getCreatedBy(), job.getJobTitle(), candidate.getFullName(), accepted);
		} catch (Exception e) {
			log.warn("Failed to send offer-response email for offer {}: {}", offer.getId(), e.getMessage());
		}
	}

	private String buildLetterHtml(Offer offer, JobCircular job, User candidate) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><meta charset='UTF-8'/><style>")
			.append("body{font-family:'Helvetica',sans-serif;color:#222;font-size:12px;margin:48px;}")
			.append("h1{font-size:20px;color:#032967;margin-bottom:0;}")
			.append(".sub{color:#666;margin-top:4px;margin-bottom:32px;}")
			.append("p{line-height:1.6;}")
			.append(".field{margin:6px 0;}")
			.append(".field-label{font-weight:bold;display:inline-block;width:160px;}")
			.append("</style></head><body>");

		sb.append("<h1>Offer of Employment</h1>");
		sb.append("<div class='sub'>").append(esc(job.getCompanyName())).append("</div>");

		sb.append("<p>Dear ").append(esc(candidate.getFullName())).append(",</p>");
		sb.append("<p>We are pleased to offer you the position of <strong>")
			.append(esc(StringUtils.defaultIfBlank(offer.getPosition(), job.getJobTitle())))
			.append("</strong> at ").append(esc(job.getCompanyName())).append(".</p>");

		sb.append("<div class='field'><span class='field-label'>Position:</span>")
			.append(esc(StringUtils.defaultIfBlank(offer.getPosition(), job.getJobTitle()))).append("</div>");
		if (StringUtils.isNotBlank(offer.getSalaryOffered())) {
			sb.append("<div class='field'><span class='field-label'>Compensation:</span>").append(esc(offer.getSalaryOffered())).append("</div>");
		}
		if (offer.getStartDate() != null) {
			sb.append("<div class='field'><span class='field-label'>Start Date:</span>").append(DATE_FORMAT.format(offer.getStartDate())).append("</div>");
		}
		if (offer.getExpiryDate() != null) {
			sb.append("<div class='field'><span class='field-label'>Offer Valid Until:</span>").append(DATE_FORMAT.format(offer.getExpiryDate())).append("</div>");
		}
		if (StringUtils.isNotBlank(offer.getNotes())) {
			sb.append("<p>").append(esc(offer.getNotes())).append("</p>");
		}

		sb.append("<p>Please review this offer and respond via your candidate dashboard by the date noted above. We look forward to welcoming you to the team.</p>");
		sb.append("<p>Sincerely,<br/>").append(esc(job.getCompanyName())).append(" Hiring Team</p>");

		sb.append("</body></html>");
		return sb.toString();
	}

	private String esc(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
