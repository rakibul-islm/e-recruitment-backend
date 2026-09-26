package com.bd.erecruitment.violation;

import com.bd.erecruitment.dto.req.McqViolationReqDto;
import com.bd.erecruitment.dto.req.SystemConfigReqDto;
import com.bd.erecruitment.dto.res.McqViolationResDTO;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.McqTestViolation;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentQuestionRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.McqTestViolationRepo;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.repository.SystemConfigRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.repository.UserSessionRepo;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.service.impl.McqTestViolationServiceImpl;
import com.bd.erecruitment.service.impl.SystemConfigServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class McqTestViolationTest {

	private static final String CANDIDATE_EMAIL = "test@e-recruitment.com";
	private static final String RECRUITER_EMAIL = "admin@e-recruitment.com";
	private static final String LIMIT_KEY = "MCQ_VIOLATION_LIMIT";
	private static final String ENABLED_KEY = "MCQ_VIOLATION_MONITORING_ENABLED";

	@MockBean
	private MailService mailService;

	@Autowired
	private McqTestViolationServiceImpl violationService;

	@Autowired
	private SystemConfigServiceImpl systemConfigService;

	@Autowired
	private UserSessionService userSessionService;

	@Autowired
	private McqTestViolationRepo violationRepo;

	@Autowired
	private McqTestAssignmentRepo assignmentRepo;

	@Autowired
	private McqTestAssignmentQuestionRepo assignmentQuestionRepo;

	@Autowired
	private McqTestRepo mcqTestRepo;

	@Autowired
	private ApplicationRepo applicationRepo;

	@Autowired
	private JobCircularRepo jobCircularRepo;

	@Autowired
	private NotificationRepo notificationRepo;

	@Autowired
	private SystemConfigRepo systemConfigRepo;

	@Autowired
	private UserSessionRepo userSessionRepo;

	@Autowired
	private UserRepo userRepo;

	private Long candidateId;
	private Long recruiterId;
	private McqTestAssignment assignment;

	@BeforeEach
	void setUp() {
		cleanUp();
		candidateId = userRepo.findByEmail(CANDIDATE_EMAIL).getId();
		recruiterId = userRepo.findByEmail(RECRUITER_EMAIL).getId();
		JobCircular job = jobCircularRepo.save(stamp(new JobCircular().setJobTitle("Backend Engineer").setStatus("PUBLISHED")));
		Application application = applicationRepo.save(stamp(new Application().setJobCircularId(job.getId())
			.setCandidateUserId(candidateId).setStatus("APPLIED")));
		McqTest test = mcqTestRepo.save(stamp(new McqTest().setName("Java basics").setDurationMinutes(30)
			.setPassingScorePercent(60).setStatus("ACTIVE")));
		assignment = assignmentRepo.save(stamp(new McqTestAssignment().setApplicationId(application.getId())
			.setMcqTestId(test.getId()).setCandidateUserId(candidateId).setStatus("IN_PROGRESS")
			.setAssignedBy(RECRUITER_EMAIL).setPassingScorePercentSnapshot(60).setCurrentQuestionIndex(0)));
		authenticateAs(candidateId, CANDIDATE_EMAIL);
	}

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
		setConfig(LIMIT_KEY, "3");
		setConfig(ENABLED_KEY, "Y");
		violationRepo.deleteAll();
		assignmentQuestionRepo.deleteAll();
		assignmentRepo.deleteAll();
		mcqTestRepo.deleteAll();
		applicationRepo.deleteAll();
		jobCircularRepo.deleteAll();
		notificationRepo.deleteAll();
	}

	@Test
	void aViolationWithinTheLimit_isRecordedAsAWarningAndTheTestKeepsRunning() {
		McqViolationResDTO result = report("TAB_HIDDEN").getObj();

		assertThat(result.isCounted()).isTrue();
		assertThat(result.getViolationCount()).isEqualTo(1);
		assertThat(result.getLimit()).isEqualTo(3);
		assertThat(result.getAction()).isEqualTo("WARNED");
		assertThat(assignmentRepo.findById(assignment.getId()).orElseThrow().getStatus()).isEqualTo("IN_PROGRESS");

		McqTestViolation row = violationRepo.findAll().get(0);
		assertThat(row.getViolationType()).isEqualTo("TAB_HIDDEN");
		assertThat(row.getSequenceNo()).isEqualTo(1);
		assertThat(row.getQuestionNumber()).isEqualTo(1);
		assertThat(row.getMcqTestId()).isEqualTo(assignment.getMcqTestId());
	}

	@Test
	void twoEventsInTheSameBurst_countAsOneIncident() {
		report("TAB_HIDDEN");
		McqViolationResDTO second = report("WINDOW_BLUR").getObj();

		assertThat(second.isCounted()).isFalse();
		assertThat(second.getViolationCount()).isEqualTo(1);
		assertThat(violationRepo.count()).isEqualTo(1);
	}

	@Test
	void crossingTheLimit_endsTheTestSubmitsItRevokesSessionsAndAlertsTheRecruiter() throws InterruptedException {
		String jti = UUID.randomUUID().toString();
		userSessionService.createSession(userRepo.findByEmail(CANDIDATE_EMAIL), jti, new Date(), farFuture());

		for (int i = 1; i <= 3; i++) {
			assertThat(report("COPY_OR_CUT").getObj().getAction()).isEqualTo("WARNED");
			backdateLastViolation();
		}
		McqViolationResDTO fourth = report("CONTEXT_MENU").getObj();

		assertThat(fourth.isCounted()).isTrue();
		assertThat(fourth.getViolationCount()).isEqualTo(4);
		assertThat(fourth.getAction()).isEqualTo("TERMINATED");

		McqTestAssignment saved = assignmentRepo.findById(assignment.getId()).orElseThrow();
		assertThat(saved.getStatus()).isEqualTo("SUBMITTED");
		assertThat(saved.getSubmittedVia()).isEqualTo("VIOLATION");
		assertThat(userSessionRepo.findByJti(jti).orElseThrow().isRevoked()).isTrue();
		assertThat(userSessionService.isActive(jti)).isFalse();

		Notification alert = awaitRow(recruiterId, NotificationType.MCQ_TEST_VIOLATION_TERMINATED);
		assertThat(alert.getActionRoute()).isEqualTo("/application-management/" + assignment.getApplicationId());
		assertThat(alert.getParamsJson()).contains("Java basics").contains("\"violationCount\":4");
	}

	@Test
	void theLimitComesFromSystemConfig() {
		setConfig(LIMIT_KEY, "1");
		authenticateAs(candidateId, CANDIDATE_EMAIL);

		assertThat(report("TAB_HIDDEN").getObj().getAction()).isEqualTo("WARNED");
		backdateLastViolation();
		McqViolationResDTO second = report("TAB_HIDDEN").getObj();

		assertThat(second.getLimit()).isEqualTo(1);
		assertThat(second.getAction()).isEqualTo("TERMINATED");
	}

	@Test
	void whenMonitoringIsDisabled_nothingIsRecorded() {
		setConfig(ENABLED_KEY, "N");
		authenticateAs(candidateId, CANDIDATE_EMAIL);

		McqViolationResDTO result = report("TAB_HIDDEN").getObj();

		assertThat(result.isCounted()).isFalse();
		assertThat(result.getAction()).isEqualTo("NONE");
		assertThat(violationRepo.count()).isZero();
	}

	@Test
	void aTestThatIsNotInProgress_ignoresViolations() {
		assignmentRepo.save(assignmentRepo.findById(assignment.getId()).orElseThrow().setStatus("SUBMITTED"));

		McqViolationResDTO result = report("TAB_HIDDEN").getObj();

		assertThat(result.isCounted()).isFalse();
		assertThat(violationRepo.count()).isZero();
	}

	@Test
	void anotherUsersAssignment_isRefused() {
		authenticateAs(recruiterId, RECRUITER_EMAIL, "SUPER_ADMIN");

		assertThatThrownBy(() -> report("TAB_HIDDEN")).isInstanceOf(ForbiddenException.class);
		assertThat(violationRepo.count()).isZero();
	}

	@Test
	void anUnknownViolationType_isRejected() {
		assertThatThrownBy(() -> report("SNEEZED")).isInstanceOf(BadRequestException.class);
		assertThat(violationRepo.count()).isZero();
	}

	private com.bd.erecruitment.util.Response<McqViolationResDTO> report(String type) {
		McqViolationReqDto request = new McqViolationReqDto();
		request.setViolationType(type);
		return violationService.record(assignment.getId(), request);
	}

	private void backdateLastViolation() {
		List<McqTestViolation> rows = violationRepo.findAll();
		McqTestViolation last = rows.get(rows.size() - 1);
		last.setCreatedOn(new Date(System.currentTimeMillis() - 10_000));
		violationRepo.save(last);
	}

	private void setConfig(String key, String value) {
		SystemConfig config = systemConfigRepo.findByConfigKeyAndDeleted(key, false).orElse(null);
		if (config == null) return;
		authenticateAs(userRepo.findByEmail(RECRUITER_EMAIL).getId(), RECRUITER_EMAIL, "SUPER_ADMIN");
		SystemConfigReqDto request = new SystemConfigReqDto();
		request.setId(config.getId());
		request.setConfigKey(key);
		request.setConfigValue(value);
		systemConfigService.update(request);
		SecurityContextHolder.clearContext();
	}

	private Notification awaitRow(Long userId, NotificationType type) throws InterruptedException {
		for (int i = 0; i < 100; i++) {
			List<Notification> rows = notificationRepo.findByRecipientUserIdAndDeletedAndIdLessThanOrderByIdDesc(userId, false, Long.MAX_VALUE, PageRequest.of(0, 50));
			for (Notification row : rows) if (row.getType() == type) return row;
			Thread.sleep(100);
		}
		throw new AssertionError("No " + type + " notification arrived for user " + userId);
	}

	private void authenticateAs(Long id, String email, String... authorities) {
		User user = User.builder().id(id).email(email).active(true).expiryDate(farFuture()).build();
		MyUserDetail principal = new MyUserDetail(user);
		List<GrantedAuthority> granted = new ArrayList<>();
		for (String authority : authorities) granted.add(new SimpleGrantedAuthority(authority));
		principal.setAuthorities(granted);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, granted));
	}

	private <T extends BaseEntity> T stamp(T entity) {
		Date now = new Date();
		entity.setCreatedBy("system").setCreatedOn(now).setUpdatedBy("system").setUpdatedOn(now).setDeleted(false);
		return entity;
	}

	private Date farFuture() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 10);
		return cal.getTime();
	}
}
