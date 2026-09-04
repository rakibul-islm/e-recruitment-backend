package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.OnboardingTaskReqDto;
import com.bd.erecruitment.dto.res.OnboardingTaskResDTO;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.OnboardingTask;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.OnboardingTaskRepo;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class OnboardingServiceImpl extends AbstractBaseService<OnboardingTask> {

	private static final String STAFF_AUTHORITY = "job-circular:write";

	private static final List<String> DEFAULT_TASKS = List.of(
		"Sign employment contract",
		"Submit ID and educational documents",
		"Complete IT equipment / account setup",
		"Attend first-day orientation"
	);

	private final OnboardingTaskRepo onboardingTaskRepo;
	private final ApplicationRepo applicationRepo;

	public OnboardingServiceImpl(OnboardingTaskRepo onboardingTaskRepo, ApplicationRepo applicationRepo) {
		super(onboardingTaskRepo);
		this.onboardingTaskRepo = onboardingTaskRepo;
		this.applicationRepo = applicationRepo;
	}

	// Called from OfferServiceImpl.respond() when a candidate accepts an offer. Runs in that
	// caller's transaction - no @Transactional here to avoid nesting a separate one.
	public void seedDefaultTasks(Long applicationId, String actor) {
		if (!onboardingTaskRepo.findAllByApplicationIdAndDeletedOrderByDueDateAsc(applicationId, false).isEmpty()) return;

		Date now = new Date();
		for (String title : DEFAULT_TASKS) {
			OnboardingTask task = new OnboardingTask().setApplicationId(applicationId).setTitle(title).setCompleted(false);
			task.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
			onboardingTaskRepo.save(task);
		}
	}

	@Transactional
	public Response<OnboardingTaskResDTO> addTask(OnboardingTaskReqDto reqDto) {
		requireStaff("manage onboarding tasks");
		if (reqDto.getApplicationId() == null) returnErrorException("Application is required");
		if (StringUtils.isBlank(reqDto.getTitle())) returnErrorException("Task title is required");

		OnboardingTask task = new OnboardingTask()
			.setApplicationId(reqDto.getApplicationId())
			.setTitle(reqDto.getTitle())
			.setDescription(reqDto.getDescription())
			.setDueDate(reqDto.getDueDate())
			.setCompleted(false);
		task = createEntity(task);
		return getCreatedResponse("Onboarding task added successfully", new OnboardingTaskResDTO(task));
	}

	public Response<OnboardingTaskResDTO> findByApplication(Long applicationId) {
		getOwnedOrStaffApplication(applicationId);
		List<OnboardingTaskResDTO> list = onboardingTaskRepo.findAllByApplicationIdAndDeletedOrderByDueDateAsc(applicationId, false)
			.stream().map(OnboardingTaskResDTO::new).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	@Transactional
	public Response<OnboardingTaskResDTO> complete(Long id) {
		OnboardingTask task = findByIdOrThrow(id, "Onboarding task not found");
		getOwnedOrStaffApplication(task.getApplicationId());

		MyUserDetail me = getLoggedInUserDetails();
		task.setCompleted(true).setCompletedOn(new Date()).setCompletedBy(me.getUsername());
		task = updateEntity(task);
		return getSuccessResponse("Task marked complete", new OnboardingTaskResDTO(task));
	}

	@Transactional
	public Response<OnboardingTaskResDTO> remove(Long id) {
		requireStaff("manage onboarding tasks");
		removeEntity(findByIdOrThrow(id, "Onboarding task not found"));
		return getSuccessResponse("Removed successfully");
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
}
