package com.bd.erecruitment.notification;

import com.bd.erecruitment.dto.req.AssignMcqTestReqDto;
import com.bd.erecruitment.dto.req.OfferResponseReqDto;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqAssignmentOptionItem;
import com.bd.erecruitment.entity.McqOptionItem;
import com.bd.erecruitment.entity.McqQuestion;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.McqTestAssignmentQuestion;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.entity.Offer;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqQuestionRepo;
import com.bd.erecruitment.repository.McqTestAssignmentQuestionRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.repository.OfferRepo;
import com.bd.erecruitment.repository.OnboardingTaskRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.impl.McqTestAssignmentServiceImpl;
import com.bd.erecruitment.service.impl.OfferServiceImpl;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NotificationWorkflowHookTest {

	private static final String CANDIDATE_EMAIL = "test@e-recruitment.com";
	private static final String RECRUITER_EMAIL = "admin@e-recruitment.com";

	@MockBean
	private MailService mailService;

	@Autowired
	private OfferServiceImpl offerService;

	@Autowired
	private McqTestAssignmentServiceImpl mcqTestAssignmentService;

	@Autowired
	private NotificationRepo notificationRepo;

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private JobCircularRepo jobCircularRepo;

	@Autowired
	private ApplicationRepo applicationRepo;

	@Autowired
	private OfferRepo offerRepo;

	@Autowired
	private OnboardingTaskRepo onboardingTaskRepo;

	@Autowired
	private McqQuestionRepo mcqQuestionRepo;

	@Autowired
	private McqTestRepo mcqTestRepo;

	@Autowired
	private McqTestAssignmentRepo mcqTestAssignmentRepo;

	@Autowired
	private McqTestAssignmentQuestionRepo mcqTestAssignmentQuestionRepo;

	private Long candidateId;
	private Long recruiterId;
	private Application application;

	@BeforeEach
	void setUp() {
		cleanUp();
		candidateId = userRepo.findByEmail(CANDIDATE_EMAIL).getId();
		recruiterId = userRepo.findByEmail(RECRUITER_EMAIL).getId();
		JobCircular job = new JobCircular().setJobTitle("Backend Engineer").setStatus("PUBLISHED");
		job.setCreatedBy(RECRUITER_EMAIL);
		job = jobCircularRepo.save(stamp(job, RECRUITER_EMAIL));
		application = applicationRepo.save(stamp(new Application().setJobCircularId(job.getId())
				.setCandidateUserId(candidateId).setStatus("APPLIED")));
	}

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
		onboardingTaskRepo.deleteAll();
		offerRepo.deleteAll();
		mcqTestAssignmentQuestionRepo.deleteAll();
		mcqTestAssignmentRepo.deleteAll();
		mcqTestRepo.deleteAll();
		mcqQuestionRepo.deleteAll();
		applicationRepo.deleteAll();
		jobCircularRepo.deleteAll();
		notificationRepo.deleteAll();
	}

	@Test
	void sendingAnOffer_notifiesTheCandidateAndNobodyElse() throws InterruptedException {
		authenticateAs(recruiterId, RECRUITER_EMAIL, "SUPER_ADMIN");
		Offer offer = saveOffer("DRAFT", 1L);

		offerService.send(offer.getId());

		Notification row = awaitRows(candidateId, 1).get(0);
		assertThat(row.getType()).isEqualTo(NotificationType.OFFER_RECEIVED);
		assertThat(row.getActionRoute()).isEqualTo("/my/applications/" + application.getId());
		assertThat(row.getParamsJson()).contains("Backend Engineer");
		assertThat(rowsFor(recruiterId)).isEmpty();
	}

	@Test
	void sendingAnOfferWithoutALetter_failsAndNotifiesNobody() throws InterruptedException {
		authenticateAs(recruiterId, RECRUITER_EMAIL, "SUPER_ADMIN");
		Offer offer = saveOffer("DRAFT", null);

		assertThatThrownBy(() -> offerService.send(offer.getId())).isInstanceOf(BadRequestException.class);

		Thread.sleep(400);
		assertThat(rowsFor(candidateId)).isEmpty();
	}

	@Test
	void acceptingAnOffer_notifiesTheRecruiterWhoOwnsTheJob() throws InterruptedException {
		authenticateAs(candidateId, CANDIDATE_EMAIL);
		Offer offer = saveOffer("SENT", 1L);

		offerService.respond(offer.getId(), response(true));

		Notification row = awaitRows(recruiterId, 1).get(0);
		assertThat(row.getType()).isEqualTo(NotificationType.OFFER_ACCEPTED);
		assertThat(row.getActionRoute()).isEqualTo("/application-management/" + application.getId());
		assertThat(row.getParamsJson()).contains("Backend Engineer").contains("Test User");
		assertThat(rowsFor(candidateId)).isEmpty();
	}

	@Test
	void decliningAnOffer_notifiesTheRecruiterWhoOwnsTheJob() throws InterruptedException {
		authenticateAs(candidateId, CANDIDATE_EMAIL);
		Offer offer = saveOffer("SENT", 1L);

		offerService.respond(offer.getId(), response(false));

		Notification row = awaitRows(recruiterId, 1).get(0);
		assertThat(row.getType()).isEqualTo(NotificationType.OFFER_DECLINED);
		assertThat(row.getActionRoute()).isEqualTo("/application-management/" + application.getId());
		assertThat(rowsFor(candidateId)).isEmpty();
	}

	@Test
	void anOfferResponseFromSomeoneOtherThanTheCandidate_isRefusedAndNotifiesNobody() throws InterruptedException {
		authenticateAs(recruiterId, RECRUITER_EMAIL, "SUPER_ADMIN");
		Offer offer = saveOffer("SENT", 1L);

		assertThatThrownBy(() -> offerService.respond(offer.getId(), response(true))).isInstanceOf(ForbiddenException.class);

		Thread.sleep(400);
		assertThat(rowsFor(recruiterId)).isEmpty();
		assertThat(rowsFor(candidateId)).isEmpty();
	}

	@Test
	void assigningATest_notifiesTheCandidateWithTheTestAndItsDuration() throws InterruptedException {
		authenticateAs(recruiterId, RECRUITER_EMAIL, "SUPER_ADMIN");
		McqQuestion question = saveQuestion();
		McqTest test = mcqTestRepo.save(stamp(new McqTest().setName("Java basics").setDurationMinutes(30)
				.setPassingScorePercent(60).setStatus("ACTIVE").setQuestionIds(List.of(question.getId()))));
		AssignMcqTestReqDto request = new AssignMcqTestReqDto();
		request.setApplicationId(application.getId());
		request.setMcqTestId(test.getId());

		mcqTestAssignmentService.assign(request);

		Notification row = awaitRows(candidateId, 1).get(0);
		assertThat(row.getType()).isEqualTo(NotificationType.MCQ_TEST_ASSIGNED);
		assertThat(row.getActionRoute()).isEqualTo("/my/applications/" + application.getId());
		assertThat(row.getParamsJson()).contains("Java basics").contains("Backend Engineer").contains("\"durationMinutes\":30");
		assertThat(rowsFor(recruiterId)).isEmpty();
	}

	@Test
	void aPassingTestResult_notifiesTheCandidateAsPassedAndTheRecruiterWithTheScore() throws InterruptedException {
		McqTest test = mcqTestRepo.save(stamp(new McqTest().setName("Java basics").setDurationMinutes(30)
				.setPassingScorePercent(60).setStatus("ACTIVE")));
		McqTestAssignment assignment = mcqTestAssignmentRepo.save(stamp(new McqTestAssignment().setApplicationId(application.getId())
				.setMcqTestId(test.getId()).setCandidateUserId(candidateId).setStatus("IN_PROGRESS")
				.setAssignedBy(RECRUITER_EMAIL).setPassingScorePercentSnapshot(60)));
		mcqTestAssignmentQuestionRepo.save(answeredQuestion(assignment.getId(), "A"));

		mcqTestAssignmentService.autoSubmitExpired(assignment.getId());

		Notification forCandidate = awaitRows(candidateId, 1).get(0);
		assertThat(forCandidate.getType()).isEqualTo(NotificationType.MCQ_TEST_PASSED);
		assertThat(forCandidate.getParamsJson()).contains("\"scorePercent\":100");

		Notification forRecruiter = awaitRows(recruiterId, 1).get(0);
		assertThat(forRecruiter.getType()).isEqualTo(NotificationType.MCQ_RESULT_RECEIVED);
		assertThat(forRecruiter.getParamsJson()).contains("\"scorePercent\":100");
	}

	@Test
	void aWrongAnswer_isReportedAsNotPassed() throws InterruptedException {
		McqTest test = mcqTestRepo.save(stamp(new McqTest().setName("Java basics").setDurationMinutes(30)
				.setPassingScorePercent(60).setStatus("ACTIVE")));
		McqTestAssignment assignment = mcqTestAssignmentRepo.save(stamp(new McqTestAssignment().setApplicationId(application.getId())
				.setMcqTestId(test.getId()).setCandidateUserId(candidateId).setStatus("IN_PROGRESS")
				.setAssignedBy(RECRUITER_EMAIL).setPassingScorePercentSnapshot(60)));
		mcqTestAssignmentQuestionRepo.save(answeredQuestion(assignment.getId(), "B"));

		mcqTestAssignmentService.autoSubmitExpired(assignment.getId());

		Notification forCandidate = awaitRows(candidateId, 1).get(0);
		assertThat(forCandidate.getType()).isEqualTo(NotificationType.MCQ_TEST_FAILED);
		assertThat(forCandidate.getParamsJson()).contains("\"scorePercent\":0");
	}

	private McqQuestion saveQuestion() {
		McqQuestion question = new McqQuestion().setQuestionText("Which keyword declares a constant?").setDifficulty("EASY")
				.setStatus("ACTIVE").setSource("MANUAL");
		question.getOptions().add(new McqOptionItem().setOptionKey("A").setOptionText("final").setCorrect(true).setDisplayOrder(0));
		question.getOptions().add(new McqOptionItem().setOptionKey("B").setOptionText("static").setCorrect(false).setDisplayOrder(1));
		return mcqQuestionRepo.save(stamp(question));
	}

	private McqTestAssignmentQuestion answeredQuestion(Long assignmentId, String selectedKey) {
		McqTestAssignmentQuestion row = new McqTestAssignmentQuestion().setAssignmentId(assignmentId).setQuestionId(1L)
				.setDisplayOrder(0).setQuestionTextSnapshot("Which keyword declares a constant?").setSelectedOptionKey(selectedKey);
		row.getOptions().add(new McqAssignmentOptionItem().setOptionKey("A").setOptionText("final").setCorrect(true).setDisplayOrder(0));
		row.getOptions().add(new McqAssignmentOptionItem().setOptionKey("B").setOptionText("static").setCorrect(false).setDisplayOrder(1));
		return stamp(row);
	}

	private Offer saveOffer(String status, Long letterFileId) {
		return offerRepo.save(stamp(new Offer().setApplicationId(application.getId()).setPosition("Backend Engineer")
				.setStatus(status).setOfferLetterFileId(letterFileId)));
	}

	private OfferResponseReqDto response(boolean accept) {
		OfferResponseReqDto response = new OfferResponseReqDto();
		response.setAccept(accept);
		return response;
	}

	private List<Notification> rowsFor(Long userId) {
		return notificationRepo.findByRecipientUserIdAndDeletedAndIdLessThanOrderByIdDesc(userId, false, Long.MAX_VALUE, PageRequest.of(0, 50));
	}

	private List<Notification> awaitRows(Long userId, int expected) throws InterruptedException {
		for (int i = 0; i < 100 && rowsFor(userId).size() < expected; i++) {
			Thread.sleep(100);
		}
		return rowsFor(userId);
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
		return stamp(entity, "system");
	}

	private <T extends BaseEntity> T stamp(T entity, String actor) {
		Date now = new Date();
		entity.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		return entity;
	}

	private Date farFuture() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 10);
		return cal.getTime();
	}
}
