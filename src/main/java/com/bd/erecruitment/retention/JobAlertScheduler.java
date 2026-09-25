package com.bd.erecruitment.retention;

import com.bd.erecruitment.dto.JobAlertItemDto;
import com.bd.erecruitment.entity.JobAlert;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.exception.ExceptionLogWriter;
import com.bd.erecruitment.notification.NotificationPublisher;
import com.bd.erecruitment.repository.JobAlertRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

// Daily digest for saved job-search alerts (JobAlert). Mirrors ArchiveScheduler's single-cron,
// per-row-try/catch shape so one broken alert/email doesn't block the rest of the run.
@Slf4j
@Component
@RequiredArgsConstructor
public class JobAlertScheduler {

	private final JobAlertRepo jobAlertRepo;
	private final JobCircularRepo jobCircularRepo;
	private final UserRepo userRepo;
	private final MailService mailService;
	private final NotificationPublisher notificationPublisher;
	private final ExceptionLogWriter exceptionLogWriter;

	private static final long SETTLE_MARGIN_MS = 60_000;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	@Scheduled(cron = "0 0 7 * * *")
	public void runDailyDigest() {
		List<JobAlert> alerts = jobAlertRepo.findAllByActiveAndDeleted(true, false);
		for (JobAlert alert : alerts) {
			try {
				processAlert(alert);
			} catch (Exception ex) {
				log.error("[JobAlertScheduler] alert {}: failed: {}", alert.getId(), ex.getMessage(), ex);
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "JobAlertScheduler:" + alert.getId());
			}
		}
	}

	// No @Transactional: it was never applied (self-invocation bypasses the proxy) and there is no DB
	// work here that should roll back with the send. Ordering is what makes this safe: the window
	// (since, upTo] is processed, the email goes out, and only then does lastNotifiedOn move to upTo.
	// If the send throws, lastNotifiedOn is untouched and the next run retries the same window.
	private void processAlert(JobAlert alert) {
		// upTo trails "now" so a job whose publishedOn was stamped just before its transaction committed
		// is still picked up by the next run instead of falling in a gap between two windows.
		Date upTo = new Date(System.currentTimeMillis() - SETTLE_MARGIN_MS);
		Date since = windowStart(alert);

		List<JobAlertItemDto> matches = jobCircularRepo.findPublishedBetween(since, upTo).stream()
			.filter(job -> matches(job, alert))
			.map(this::toJobAlertItem)
			.toList();

		if (!matches.isEmpty()) {
			User user = userRepo.findByIdAndDeleted(alert.getUserId(), false).orElse(null);
			if (user != null) {
				mailService.sendJobAlertDigestEmail(user.getEmail(), user.getFullName(), matches);
				notificationPublisher.notifyUser(user.getId(), NotificationType.JOB_ALERT_MATCH, "/jobs", "count", matches.size());
				log.info("[JobAlertScheduler] alert {}: sent digest of {} job(s) to {}", alert.getId(), matches.size(), user.getEmail());
			}
		}

		alert.setLastNotifiedOn(upTo);
		jobAlertRepo.save(alert);
	}

	// Never look back further than the default window, so an alert whose emails keep failing can't
	// accumulate an ever-growing digest.
	private Date windowStart(JobAlert alert) {
		Date floor = defaultLookback();
		Date last = alert.getLastNotifiedOn();
		return last != null && last.after(floor) ? last : floor;
	}

	private JobAlertItemDto toJobAlertItem(JobCircular job) {
		String employmentType = StringUtils.defaultIfBlank(job.getEmploymentStatus(), job.getWorkPlace());
		String deadline = job.getApplicationDeadLine() != null
			? new SimpleDateFormat("dd MMM yyyy").format(job.getApplicationDeadLine())
			: null;
		return new JobAlertItemDto(job.getJobTitle(), job.getCompanyName(), job.getJobLocation(), employmentType,
			deadline, frontendBaseUrl + "/jobs/" + job.getId());
	}

	// Needles are trimmed here as well as on save, so alerts stored before save() started trimming
	// still match.
	private boolean matches(JobCircular job, JobAlert alert) {
		String keyword = StringUtils.trimToNull(alert.getKeyword());
		String location = StringUtils.trimToNull(alert.getLocation());
		String category = StringUtils.trimToNull(alert.getCategory());
		if (keyword != null && !containsIgnoreCase(job.getJobTitle(), keyword) && !containsIgnoreCase(job.getSkills(), keyword)) {
			return false;
		}
		if (location != null && !containsIgnoreCase(job.getJobLocation(), location)) {
			return false;
		}
		if (category != null && !containsIgnoreCase(job.getCategory(), category)) {
			return false;
		}
		return true;
	}

	private boolean containsIgnoreCase(String haystack, String needle) {
		return StringUtils.isNotBlank(haystack) && haystack.toLowerCase().contains(needle.toLowerCase());
	}

	// A never-notified alert only picks up jobs published in the last 30 days, not the platform's
	// entire history.
	private Date defaultLookback() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -30);
		return cal.getTime();
	}
}
