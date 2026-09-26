package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.McqViolationReqDto;
import com.bd.erecruitment.dto.res.McqViolationResDTO;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.McqTestViolation;
import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.enums.McqViolationType;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.exception.ExceptionLogWriter;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.notification.NotificationPublisher;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.McqTestViolationRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.util.RequestUtils;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class McqTestViolationServiceImpl extends CommonFunctionsImpl {

	private static final String MONITORING_ENABLED_KEY = "MCQ_VIOLATION_MONITORING_ENABLED";
	private static final String LIMIT_KEY = "MCQ_VIOLATION_LIMIT";
	private static final int DEFAULT_LIMIT = 3;
	// Tab switches fire visibilitychange and blur together; one burst is one incident.
	private static final long BURST_WINDOW_MS = 2000;
	private static final String LOGOUT_REASON = "mcq-violation";

	private static final String ACTION_NONE = "NONE";
	private static final String ACTION_WARNED = "WARNED";
	private static final String ACTION_TERMINATED = "TERMINATED";

	private final McqTestViolationRepo violationRepo;
	private final McqTestAssignmentRepo assignmentRepo;
	private final McqTestAssignmentServiceImpl assignmentService;
	private final McqTestRepo mcqTestRepo;
	private final ApplicationRepo applicationRepo;
	private final JobCircularRepo jobCircularRepo;
	private final UserRepo userRepo;
	private final SystemConfigServiceImpl systemConfigService;
	private final UserSessionService userSessionService;
	private final NotificationPublisher notificationPublisher;
	private final ExceptionLogWriter exceptionLogWriter;

	@Transactional
	public Response<McqViolationResDTO> record(Long assignmentId, McqViolationReqDto reqDto) {
		MyUserDetail me = currentUser();
		McqTestAssignment assignment = assignmentRepo.findByIdAndDeleted(assignmentId, false)
			.orElseThrow(() -> new NotFoundException("Assignment not found"));
		if (me == null || !assignment.getCandidateUserId().equals(me.getId())) throw new ForbiddenException("Access denied");

		int limit = violationLimit();
		int count = (int) violationRepo.countByAssignmentIdAndDeleted(assignmentId, false);
		McqViolationType type = parseType(reqDto.getViolationType());

		if (!monitoringEnabled() || !"IN_PROGRESS".equals(assignment.getStatus()) || isBurst(assignmentId)) {
			return getSuccessResponse("Not counted", new McqViolationResDTO(false, count, limit, ACTION_NONE));
		}

		int sequenceNo = count + 1;
		boolean terminate = sequenceNo > limit;
		String action = terminate ? ACTION_TERMINATED : ACTION_WARNED;
		saveViolation(assignment, me, type, reqDto.getDetail(), sequenceNo, action);

		if (terminate) terminate(assignment, me, sequenceNo);
		return getSuccessResponse("Violation recorded", new McqViolationResDTO(true, sequenceNo, limit, action));
	}

	private void terminate(McqTestAssignment assignment, MyUserDetail me, int violationCount) {
		McqTestAssignment submitted = assignmentService.terminateForViolations(assignment);
		notifyStaff(submitted, violationCount);
		userSessionService.forceLogoutUser(me.getId(), LOGOUT_REASON);
	}

	private void saveViolation(McqTestAssignment assignment, MyUserDetail me, McqViolationType type, String detail, int sequenceNo, String action) {
		Date now = new Date();
		McqTestViolation violation = new McqTestViolation()
			.setAssignmentId(assignment.getId())
			.setMcqTestId(assignment.getMcqTestId())
			.setCandidateUserId(me.getId())
			.setViolationType(type.name())
			.setDetail(StringUtils.abbreviate(detail, 100))
			.setQuestionNumber(assignment.getCurrentQuestionIndex() != null ? assignment.getCurrentQuestionIndex() + 1 : 1)
			.setSequenceNo(sequenceNo)
			.setAction(action)
			.setIpAddress(RequestUtils.getClientTerminal())
			.setUserAgent(StringUtils.abbreviate(RequestUtils.getUserAgent(), 255));
		violation.setCreatedBy(me.getUsername()).setCreatedOn(now).setCreatedTerminal(RequestUtils.getClientTerminal())
			.setUpdatedBy(me.getUsername()).setUpdatedOn(now).setDeleted(false);
		violationRepo.save(violation);
	}

	private boolean isBurst(Long assignmentId) {
		return violationRepo.findFirstByAssignmentIdAndDeletedOrderByIdDesc(assignmentId, false)
			.map(last -> new Date().getTime() - last.getCreatedOn().getTime() < BURST_WINDOW_MS)
			.orElse(false);
	}

	private McqViolationType parseType(String value) {
		try {
			return McqViolationType.valueOf(StringUtils.upperCase(StringUtils.trimToEmpty(value)));
		} catch (IllegalArgumentException e) {
			returnErrorException("Unknown violation type");
			return null;
		}
	}

	private boolean monitoringEnabled() {
		SystemConfig config = systemConfigService.findCachedByKey(MONITORING_ENABLED_KEY);
		return config == null || "Y".equalsIgnoreCase(config.getConfigValue());
	}

	private int violationLimit() {
		SystemConfig config = systemConfigService.findCachedByKey(LIMIT_KEY);
		if (config == null) return DEFAULT_LIMIT;
		try {
			return Math.max(0, Integer.parseInt(config.getConfigValue().trim()));
		} catch (NumberFormatException e) {
			return DEFAULT_LIMIT;
		}
	}

	private void notifyStaff(McqTestAssignment assignment, int violationCount) {
		try {
			Application application = applicationRepo.findByIdAndDeleted(assignment.getApplicationId(), false).orElse(null);
			User candidate = userRepo.findByIdAndDeleted(assignment.getCandidateUserId(), false).orElse(null);
			McqTest test = mcqTestRepo.findByIdAndDeleted(assignment.getMcqTestId(), false).orElse(null);
			if (application == null || candidate == null || test == null) return;
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			notificationPublisher.notifyEmail(assignment.getAssignedBy(), NotificationType.MCQ_TEST_VIOLATION_TERMINATED,
				"/application-management/" + application.getId(), "candidateName", candidate.getFullName(),
				"jobTitle", job != null ? job.getJobTitle() : "", "testName", test.getName(), "violationCount", violationCount);
		} catch (Exception e) {
			log.warn("Failed to notify staff about violation termination of assignment {}: {}", assignment.getId(), e.getMessage());
			exceptionLogWriter.log(e, 0, e.getMessage(), "McqTestViolationServiceImpl.notifyStaff:" + assignment.getId());
		}
	}

	private MyUserDetail currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return null;
		return auth.getPrincipal() instanceof MyUserDetail mud ? mud : null;
	}
}
