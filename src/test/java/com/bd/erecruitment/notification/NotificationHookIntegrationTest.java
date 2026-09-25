package com.bd.erecruitment.notification;

import com.bd.erecruitment.dto.req.RecruiterApplicationReqDto;
import com.bd.erecruitment.dto.res.RecruiterApplicationResDTO;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.JobAlert;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.JobAlertRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.repository.RecruiterApplicationRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.retention.JobAlertScheduler;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.impl.McqTestAssignmentServiceImpl;
import com.bd.erecruitment.service.impl.RecruiterApplicationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class NotificationHookIntegrationTest {

	private static final String CANDIDATE_EMAIL = "test@e-recruitment.com";
	private static final String RECRUITER_EMAIL = "admin@e-recruitment.com";

	@MockBean
	private MailService mailService;

	@Autowired
	private NotificationRepo notificationRepo;

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private RecruiterApplicationServiceImpl recruiterApplicationService;

	@Autowired
	private RecruiterApplicationRepo recruiterApplicationRepo;

	@Autowired
	private McqTestAssignmentServiceImpl mcqTestAssignmentService;

	@Autowired
	private McqTestAssignmentRepo mcqTestAssignmentRepo;

	@Autowired
	private McqTestRepo mcqTestRepo;

	@Autowired
	private ApplicationRepo applicationRepo;

	@Autowired
	private JobCircularRepo jobCircularRepo;

	@Autowired
	private JobAlertRepo jobAlertRepo;

	@Autowired
	private JobAlertScheduler jobAlertScheduler;

	private Long candidateId;
	private Long recruiterId;

	@BeforeEach
	void setUp() {
		cleanUp();
		candidateId = userRepo.findByEmail(CANDIDATE_EMAIL).getId();
		recruiterId = userRepo.findByEmail(RECRUITER_EMAIL).getId();
	}

	@AfterEach
	void cleanUp() {
		jobAlertRepo.deleteAll();
		mcqTestAssignmentRepo.deleteAll();
		mcqTestRepo.deleteAll();
		applicationRepo.deleteAll();
		jobCircularRepo.deleteAll();
		recruiterApplicationRepo.deleteAll();
		notificationRepo.deleteAll();
	}

	@Test
	void recruiterApplicationSubmission_notifiesReviewersAndNobodyElse() throws InterruptedException {
		RecruiterApplicationReqDto form = new RecruiterApplicationReqDto();
		form.setFullName("Nadia Rahman");
		form.setEmail("nadia.recruiter@example.com");
		form.setCompanyName("Acme Ltd");

		Long applicationId = recruiterApplicationService.save(form).getObj().getId();

		List<Notification> reviewerRows = awaitRows(recruiterId, 1);
		assertThat(reviewerRows).hasSize(1);
		assertThat(reviewerRows.get(0).getType()).isEqualTo(NotificationType.RECRUITER_APPLICATION_SUBMITTED);
		assertThat(reviewerRows.get(0).getActionRoute()).isEqualTo("/recruiter-applications/" + applicationId);
		assertThat(reviewerRows.get(0).getParamsJson()).contains("Acme Ltd").contains("Nadia Rahman");
		assertThat(rowsFor(candidateId)).isEmpty();
	}

	@Test
	void completedMcqTest_notifiesTheCandidateAndTheRecruiterWhoAssignedIt() throws InterruptedException {
		JobCircular job = jobCircularRepo.save(stamp(new JobCircular().setJobTitle("Backend Engineer").setStatus("PUBLISHED")));
		Application application = applicationRepo.save(stamp(new Application().setJobCircularId(job.getId())
				.setCandidateUserId(candidateId).setStatus("SCREENING")));
		McqTest test = mcqTestRepo.save(stamp(new McqTest().setName("Java basics").setDurationMinutes(30)
				.setPassingScorePercent(60).setStatus("PUBLISHED")));
		McqTestAssignment assignment = mcqTestAssignmentRepo.save(stamp(new McqTestAssignment().setApplicationId(application.getId())
				.setMcqTestId(test.getId()).setCandidateUserId(candidateId).setStatus("IN_PROGRESS").setAssignedBy(RECRUITER_EMAIL)));

		mcqTestAssignmentService.autoSubmitExpired(assignment.getId());

		Notification forCandidate = awaitRows(candidateId, 1).get(0);
		assertThat(forCandidate.getType()).isEqualTo(NotificationType.MCQ_TEST_FAILED);
		assertThat(forCandidate.getActionRoute()).isEqualTo("/my/applications/" + application.getId());

		Notification forRecruiter = awaitRows(recruiterId, 1).get(0);
		assertThat(forRecruiter.getType()).isEqualTo(NotificationType.MCQ_RESULT_RECEIVED);
		assertThat(forRecruiter.getActionRoute()).isEqualTo("/application-management/" + application.getId());
		assertThat(forRecruiter.getParamsJson()).contains("Java basics").contains("Backend Engineer");
	}

	@Test
	void jobAlertDigest_alsoCreatesAnInAppNotificationWithTheMatchCount() throws InterruptedException {
		jobCircularRepo.save(publishedJob("Senior Java Developer", 5));
		jobAlertRepo.save(stamp(new JobAlert().setUserId(candidateId).setKeyword("Java").setActive(true)));

		jobAlertScheduler.runDailyDigest();

		Notification row = awaitRows(candidateId, 1).get(0);
		assertThat(row.getType()).isEqualTo(NotificationType.JOB_ALERT_MATCH);
		assertThat(row.getActionRoute()).isEqualTo("/jobs");
		assertThat(row.getParamsJson()).contains("\"count\":1");
		verify(mailService).sendJobAlertDigestEmail(anyString(), any(), anyList());
	}

	@Test
	void jobAlertDigest_ignoresLeadingAndTrailingSpacesInTheStoredKeyword() {
		jobCircularRepo.save(publishedJob("English Teacher", 5));
		jobAlertRepo.save(stamp(new JobAlert().setUserId(candidateId).setKeyword("Teacher ").setActive(true)));

		jobAlertScheduler.runDailyDigest();

		verify(mailService).sendJobAlertDigestEmail(anyString(), any(), anyList());
	}

	@Test
	void jobAlertDigest_doesNotAdvanceTheAlertWhenTheEmailFails_soTheNextRunRetries() {
		jobCircularRepo.save(publishedJob("Senior Java Developer", 5));
		JobAlert alert = jobAlertRepo.save(stamp(new JobAlert().setUserId(candidateId).setKeyword("Java").setActive(true)));
		doThrow(new IllegalStateException("gmail down")).when(mailService).sendJobAlertDigestEmail(anyString(), any(), anyList());

		jobAlertScheduler.runDailyDigest();

		assertThat(jobAlertRepo.findById(alert.getId()).orElseThrow().getLastNotifiedOn()).isNull();
		assertThat(rowsFor(candidateId)).isEmpty();

		doNothing().when(mailService).sendJobAlertDigestEmail(anyString(), any(), anyList());
		jobAlertScheduler.runDailyDigest();

		verify(mailService, times(2)).sendJobAlertDigestEmail(anyString(), any(), anyList());
		assertThat(jobAlertRepo.findById(alert.getId()).orElseThrow().getLastNotifiedOn()).isNotNull();
	}

	@Test
	void jobAlertDigest_doesNotRepeatAJobJustBecauseItWasEditedAfterTheDigest() {
		JobCircular job = jobCircularRepo.save(publishedJob("Senior Java Developer", 5));
		jobAlertRepo.save(stamp(new JobAlert().setUserId(candidateId).setKeyword("Java").setActive(true)));

		jobAlertScheduler.runDailyDigest();
		verify(mailService, times(1)).sendJobAlertDigestEmail(anyString(), any(), anyList());

		job.setUpdatedOn(new Date());
		jobCircularRepo.save(job);
		jobAlertScheduler.runDailyDigest();

		verify(mailService, times(1)).sendJobAlertDigestEmail(anyString(), any(), anyList());
	}

	@Test
	void jobAlertDigest_holdsBackAJobPublishedInsideTheSettleMargin() {
		JobCircular fresh = stamp(new JobCircular().setJobTitle("Senior Java Developer").setStatus("PUBLISHED"));
		fresh.setPublishedOn(new Date());
		jobCircularRepo.save(fresh);
		jobAlertRepo.save(stamp(new JobAlert().setUserId(candidateId).setKeyword("Java").setActive(true)));

		jobAlertScheduler.runDailyDigest();

		verify(mailService, never()).sendJobAlertDigestEmail(anyString(), any(), anyList());
	}

	private JobCircular publishedJob(String title, int publishedMinutesAgo) {
		Date published = new Date(System.currentTimeMillis() - publishedMinutesAgo * 60_000L);
		JobCircular job = stamp(new JobCircular().setJobTitle(title).setStatus("PUBLISHED"));
		job.setPublishedOn(published);
		job.setCreatedOn(published);
		return job;
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

	private <T extends BaseEntity> T stamp(T entity) {
		Date now = new Date();
		entity.setCreatedBy("system").setCreatedOn(now).setUpdatedBy("system").setUpdatedOn(now).setDeleted(false);
		return entity;
	}
}
