package com.bd.erecruitment.notification;

import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.Interview;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.entity.OnboardingTask;
import com.bd.erecruitment.entity.SavedJob;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.InterviewRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.repository.OnboardingTaskRepo;
import com.bd.erecruitment.repository.SavedJobRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.retention.NotificationReminderScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationReminderSchedulerTest {

	@Autowired
	private NotificationReminderScheduler scheduler;

	@Autowired
	private NotificationRepo notificationRepo;

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private JobCircularRepo jobCircularRepo;

	@Autowired
	private ApplicationRepo applicationRepo;

	@Autowired
	private InterviewRepo interviewRepo;

	@Autowired
	private McqTestRepo mcqTestRepo;

	@Autowired
	private McqTestAssignmentRepo mcqTestAssignmentRepo;

	@Autowired
	private SavedJobRepo savedJobRepo;

	@Autowired
	private OnboardingTaskRepo onboardingTaskRepo;

	private Long candidateId;
	private Long otherUserId;
	private JobCircular job;
	private Application application;

	@BeforeEach
	void setUp() {
		cleanUp();
		candidateId = userRepo.findByEmail("test@e-recruitment.com").getId();
		otherUserId = userRepo.findByEmail("admin@e-recruitment.com").getId();
		job = saveJob("Backend Engineer", "PUBLISHED", LocalDate.now(ZoneOffset.UTC).plusDays(30));
		application = saveApplication(job, candidateId);
	}

	@AfterEach
	void cleanUp() {
		onboardingTaskRepo.deleteAll();
		savedJobRepo.deleteAll();
		mcqTestAssignmentRepo.deleteAll();
		mcqTestRepo.deleteAll();
		interviewRepo.deleteAll();
		applicationRepo.deleteAll();
		jobCircularRepo.deleteAll();
		notificationRepo.deleteAll();
	}

	@Test
	void interviewWithin24Hours_remindsCandidateAndInterviewerOnlyOnce() {
		Interview interview = saveInterview(inFuture(Duration.ofHours(2)), "SCHEDULED");

		scheduler.run();
		scheduler.run();

		List<Notification> candidateRows = rowsFor(candidateId);
		assertThat(candidateRows).hasSize(1);
		assertThat(candidateRows.get(0).getType()).isEqualTo(NotificationType.INTERVIEW_REMINDER);
		assertThat(candidateRows.get(0).getActionRoute()).isEqualTo("/my/applications/" + application.getId());
		assertThat(candidateRows.get(0).getParamsJson()).contains("Backend Engineer").contains(interview.getTitle());
		assertThat(candidateRows.get(0).getParamsJson()).containsPattern("\"scheduledAt\":\"\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z\"");

		List<Notification> interviewerRows = rowsFor(otherUserId);
		assertThat(interviewerRows).hasSize(1);
		assertThat(interviewerRows.get(0).getActionRoute()).isEqualTo("/application-management/" + application.getId());
	}

	@Test
	void interviewOutsideTheWindowOrNotScheduled_isIgnored() {
		saveInterview(inFuture(Duration.ofDays(3)), "SCHEDULED");
		saveInterview(inFuture(Duration.ofHours(2)), "CANCELLED");
		saveInterview(inFuture(Duration.ofHours(-1)), "SCHEDULED");

		scheduler.run();

		assertThat(rowsFor(candidateId)).isEmpty();
		assertThat(rowsFor(otherUserId)).isEmpty();
	}

	@Test
	void rescheduledInterview_remindsAgainForTheNewTime() {
		Interview interview = saveInterview(inFuture(Duration.ofHours(2)), "SCHEDULED");
		scheduler.run();

		interview.setScheduledAt(inFuture(Duration.ofHours(5)));
		interviewRepo.save(interview);
		scheduler.run();

		assertThat(rowsFor(candidateId)).hasSize(2);
	}

	@Test
	void testWindowClosingWithin24Hours_remindsCandidateOnlyWhileStillAssigned() {
		McqTest test = mcqTestRepo.save(stamp(new McqTest().setName("Java basics").setDurationMinutes(30)
				.setPassingScorePercent(60).setStatus("PUBLISHED")));
		saveAssignment(test, "ASSIGNED", inFuture(Duration.ofHours(3)));
		saveAssignment(test, "IN_PROGRESS", inFuture(Duration.ofHours(3)));
		saveAssignment(test, "ASSIGNED", inFuture(Duration.ofDays(4)));

		scheduler.run();
		scheduler.run();

		List<Notification> rows = rowsFor(candidateId);
		assertThat(rows).hasSize(1);
		assertThat(rows.get(0).getType()).isEqualTo(NotificationType.MCQ_TEST_CLOSING);
		assertThat(rows.get(0).getParamsJson()).contains("Java basics").contains("Backend Engineer");
	}

	@Test
	void savedJobClosingSoon_remindsSaversUnlessTheyAlreadyApplied() {
		LocalDate closing = LocalDate.now(ZoneOffset.UTC).plusDays(2);
		JobCircular closingSoon = saveJob("Data Analyst", "PUBLISHED", closing);
		JobCircular alreadyApplied = saveJob("QA Engineer", "PUBLISHED", closing);
		JobCircular farAway = saveJob("Designer", "PUBLISHED", LocalDate.now(ZoneOffset.UTC).plusDays(10));
		JobCircular draft = saveJob("Writer", "DRAFT", closing);
		saveApplication(alreadyApplied, candidateId);
		for (JobCircular saved : List.of(closingSoon, alreadyApplied, farAway, draft)) {
			saveJobFor(candidateId, saved);
		}
		saveJobFor(otherUserId, closingSoon);

		scheduler.run();
		scheduler.run();

		List<Notification> candidateRows = rowsFor(candidateId);
		assertThat(candidateRows).hasSize(1);
		assertThat(candidateRows.get(0).getType()).isEqualTo(NotificationType.JOB_DEADLINE_SOON);
		assertThat(candidateRows.get(0).getActionRoute()).isEqualTo("/jobs/" + closingSoon.getId());
		assertThat(candidateRows.get(0).getParamsJson()).contains("Data Analyst").contains(closing.toString());
		assertThat(rowsFor(otherUserId)).hasSize(1);
	}

	@Test
	void onboardingTaskDueSoonOrRecentlyOverdue_remindsCandidateButNotCompletedOrAncientOnes() {
		saveTask("Sign contract", LocalDate.now(ZoneOffset.UTC).plusDays(1), false);
		saveTask("Collect laptop", LocalDate.now(ZoneOffset.UTC).minusDays(5), false);
		saveTask("Already done", LocalDate.now(ZoneOffset.UTC).plusDays(1), true);
		saveTask("Ancient", LocalDate.now(ZoneOffset.UTC).minusDays(60), false);
		saveTask("Far away", LocalDate.now(ZoneOffset.UTC).plusDays(20), false);

		scheduler.run();
		scheduler.run();

		List<Notification> rows = rowsFor(candidateId);
		assertThat(rows).hasSize(2);
		assertThat(rows).allMatch(row -> row.getType() == NotificationType.ONBOARDING_TASK_DUE);
		assertThat(rows).extracting(Notification::getParamsJson)
				.anyMatch(json -> json.contains("Sign contract")).anyMatch(json -> json.contains("Collect laptop"));
	}

	private List<Notification> rowsFor(Long userId) {
		return notificationRepo.findByRecipientUserIdAndDeletedAndIdLessThanOrderByIdDesc(userId, false, Long.MAX_VALUE, PageRequest.of(0, 50));
	}

	private JobCircular saveJob(String title, String status, LocalDate deadline) {
		return jobCircularRepo.save(stamp(new JobCircular().setJobTitle(title).setStatus(status)
				.setApplicationDeadLine(utcMidnight(deadline))));
	}

	private Application saveApplication(JobCircular forJob, Long candidate) {
		return applicationRepo.save(stamp(new Application().setJobCircularId(forJob.getId()).setCandidateUserId(candidate).setStatus("APPLIED")));
	}

	private Interview saveInterview(Date scheduledAt, String status) {
		return interviewRepo.save(stamp(new Interview().setApplicationId(application.getId()).setTitle("Technical round")
				.setScheduledAt(scheduledAt).setStatus(status).setInterviewerUserIds(List.of(otherUserId))));
	}

	private McqTestAssignment saveAssignment(McqTest test, String status, Date scheduledEndAt) {
		return mcqTestAssignmentRepo.save(stamp(new McqTestAssignment().setApplicationId(application.getId()).setMcqTestId(test.getId())
				.setCandidateUserId(candidateId).setStatus(status).setScheduledEndAt(scheduledEndAt)));
	}

	private void saveJobFor(Long userId, JobCircular saved) {
		savedJobRepo.save(stamp(new SavedJob().setUserId(userId).setJobCircularId(saved.getId())));
	}

	private void saveTask(String title, LocalDate dueDate, boolean completed) {
		onboardingTaskRepo.save(stamp(new OnboardingTask().setApplicationId(application.getId()).setTitle(title)
				.setDueDate(utcMidnight(dueDate)).setCompleted(completed)));
	}

	private Date utcMidnight(LocalDate date) {
		return Date.from(date.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

	private Date inFuture(Duration offset) {
		return Date.from(Instant.now().plus(offset));
	}

	private <T extends BaseEntity> T stamp(T entity) {
		Date now = new Date();
		entity.setCreatedBy("system").setCreatedOn(now).setUpdatedBy("system").setUpdatedOn(now).setDeleted(false);
		return entity;
	}
}
