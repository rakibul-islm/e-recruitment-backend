package com.bd.erecruitment.retention;

import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.Interview;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.OnboardingTask;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.notification.NotificationEvent;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.InterviewRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.OnboardingTaskRepo;
import com.bd.erecruitment.repository.SavedJobRepo;
import com.bd.erecruitment.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationReminderScheduler {

	private static final Duration INTERVIEW_WINDOW = Duration.ofHours(24);
	private static final Duration TEST_WINDOW = Duration.ofHours(24);
	private static final Duration JOB_DEADLINE_WINDOW = Duration.ofDays(3);
	private static final Duration ONBOARDING_WINDOW = Duration.ofHours(24);
	private static final Duration ONBOARDING_LOOKBACK = Duration.ofDays(30);

	private final InterviewRepo interviewRepo;
	private final McqTestAssignmentRepo mcqTestAssignmentRepo;
	private final McqTestRepo mcqTestRepo;
	private final JobCircularRepo jobCircularRepo;
	private final SavedJobRepo savedJobRepo;
	private final ApplicationRepo applicationRepo;
	private final OnboardingTaskRepo onboardingTaskRepo;
	private final NotificationServiceImpl notificationService;

	@Scheduled(cron = "0 */5 * * * *")
	public void run() {
		Instant now = Instant.now();
		runSafely("interview", () -> remindInterviews(now));
		runSafely("mcq test", () -> remindTests(now));
		runSafely("job deadline", () -> remindJobDeadlines(now));
		runSafely("onboarding", () -> remindOnboarding(now));
	}

	private void remindInterviews(Instant now) {
		List<Interview> interviews = interviewRepo.findWithInterviewersByStatusAndScheduledAtBetween(
			"SCHEDULED", Date.from(now), Date.from(now.plus(INTERVIEW_WINDOW)));
		forEachSafely(interviews, this::remindInterview);
	}

	private void remindInterview(Interview interview) {
		Application application = applicationRepo.findByIdAndDeleted(interview.getApplicationId(), false).orElse(null);
		if (application == null) return;

		String key = "INTERVIEW_REMINDER:" + interview.getId() + ":" + minute(interview.getScheduledAt());
		Object[] params = { "jobTitle", jobTitle(application), "interviewTitle", interview.getTitle(), "scheduledAt", iso(interview.getScheduledAt()) };
		remind(application.getCandidateUserId(), NotificationType.INTERVIEW_REMINDER, "/my/applications/" + application.getId(), key, params);
		for (Long interviewerId : interview.getInterviewerUserIds()) {
			remind(interviewerId, NotificationType.INTERVIEW_REMINDER, "/application-management/" + application.getId(), key, params);
		}
	}

	private void remindTests(Instant now) {
		List<McqTestAssignment> assignments = mcqTestAssignmentRepo.findAllByStatusAndScheduledEndAtBetweenAndDeleted(
			"ASSIGNED", Date.from(now), Date.from(now.plus(TEST_WINDOW)), false);
		forEachSafely(assignments, this::remindTest);
	}

	private void remindTest(McqTestAssignment assignment) {
		Application application = applicationRepo.findByIdAndDeleted(assignment.getApplicationId(), false).orElse(null);
		McqTest test = mcqTestRepo.findByIdAndDeleted(assignment.getMcqTestId(), false).orElse(null);
		if (application == null || test == null) return;

		String key = "MCQ_TEST_CLOSING:" + assignment.getId() + ":" + minute(assignment.getScheduledEndAt());
		remind(assignment.getCandidateUserId(), NotificationType.MCQ_TEST_CLOSING, "/my/applications/" + application.getId(), key,
			"jobTitle", jobTitle(application), "testName", test.getName(), "closesAt", iso(assignment.getScheduledEndAt()));
	}

	private void remindJobDeadlines(Instant now) {
		Date startOfToday = Date.from(LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC));
		List<JobCircular> jobs = jobCircularRepo.findAllByStatusAndApplicationDeadLineBetweenAndDeleted(
			"PUBLISHED", startOfToday, Date.from(now.plus(JOB_DEADLINE_WINDOW)), false);
		forEachSafely(jobs, this::remindJobDeadline);
	}

	private void remindJobDeadline(JobCircular job) {
		String key = "JOB_DEADLINE_SOON:" + job.getId() + ":" + day(job.getApplicationDeadLine());
		forEachSafely(savedJobRepo.findAllByJobCircularIdAndDeleted(job.getId(), false), saved -> {
			if (applicationRepo.findByJobCircularIdAndCandidateUserIdAndDeleted(job.getId(), saved.getUserId(), false).isPresent()) return;
			remind(saved.getUserId(), NotificationType.JOB_DEADLINE_SOON, "/jobs/" + job.getId(), key,
				"jobTitle", job.getJobTitle(), "deadlineDate", utcDate(job.getApplicationDeadLine()));
		});
	}

	private void remindOnboarding(Instant now) {
		List<OnboardingTask> tasks = onboardingTaskRepo.findAllByCompletedAndDueDateBetweenAndDeleted(
			false, Date.from(now.minus(ONBOARDING_LOOKBACK)), Date.from(now.plus(ONBOARDING_WINDOW)), false);
		forEachSafely(tasks, this::remindOnboardingTask);
	}

	private void remindOnboardingTask(OnboardingTask task) {
		Application application = applicationRepo.findByIdAndDeleted(task.getApplicationId(), false).orElse(null);
		if (application == null) return;

		remind(application.getCandidateUserId(), NotificationType.ONBOARDING_TASK_DUE, "/my/applications/" + application.getId(),
			"ONBOARDING_TASK_DUE:" + task.getId() + ":" + day(task.getDueDate()), "title", task.getTitle(), "dueDate", utcDate(task.getDueDate()));
	}

	private void remind(Long userId, NotificationType type, String route, String dedupeKey, Object... keyValues) {
		if (userId == null) return;
		notificationService.create(NotificationEvent.toUser(userId, type, route, keyValues).withDedupeKey(dedupeKey));
	}

	private String jobTitle(Application application) {
		return jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).map(JobCircular::getJobTitle).orElse(null);
	}

	private <T> void forEachSafely(List<T> rows, Consumer<T> action) {
		for (T row : rows) {
			try {
				action.accept(row);
			} catch (Exception e) {
				log.warn("[NotificationReminderScheduler] {} failed: {}", row.getClass().getSimpleName(), e.getMessage(), e);
			}
		}
	}

	private void runSafely(String label, Runnable action) {
		try {
			action.run();
		} catch (Exception e) {
			log.error("[NotificationReminderScheduler] {} reminders failed: {}", label, e.getMessage(), e);
		}
	}

	private long minute(Date date) {
		return date.getTime() / 60000;
	}

	private long day(Date date) {
		return date.getTime() / 86400000;
	}

	private String iso(Date date) {
		return Instant.ofEpochMilli(date.getTime()).toString();
	}

	private String utcDate(Date date) {
		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneOffset.UTC).toLocalDate().toString();
	}
}
