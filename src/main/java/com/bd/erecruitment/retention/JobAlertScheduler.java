package com.bd.erecruitment.retention;

import com.bd.erecruitment.entity.JobAlert;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.repository.JobAlertRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

	@Scheduled(cron = "0 0 7 * * *")
	public void runDailyDigest() {
		List<JobAlert> alerts = jobAlertRepo.findAllByActiveAndDeleted(true, false);
		for (JobAlert alert : alerts) {
			try {
				processAlert(alert);
			} catch (Exception ex) {
				log.error("[JobAlertScheduler] alert {}: failed: {}", alert.getId(), ex.getMessage(), ex);
			}
		}
	}

	@Transactional
	void processAlert(JobAlert alert) {
		Date since = alert.getLastNotifiedOn() != null ? alert.getLastNotifiedOn() : defaultLookback();
		List<JobCircular> candidates = jobCircularRepo.findAllByStatusAndUpdatedOnAfterAndDeleted("PUBLISHED", since, false);

		List<String> matches = candidates.stream()
			.filter(job -> matches(job, alert))
			.map(JobCircular::getJobTitle)
			.toList();

		alert.setLastNotifiedOn(new Date());
		jobAlertRepo.save(alert);

		if (matches.isEmpty()) return;

		User user = userRepo.findByIdAndDeleted(alert.getUserId(), false).orElse(null);
		if (user == null) return;

		mailService.sendJobAlertDigestEmail(user.getEmail(), user.getFullName(), matches);
		log.info("[JobAlertScheduler] alert {}: sent digest of {} job(s) to {}", alert.getId(), matches.size(), user.getEmail());
	}

	private boolean matches(JobCircular job, JobAlert alert) {
		if (StringUtils.isNotBlank(alert.getKeyword()) && !containsIgnoreCase(job.getJobTitle(), alert.getKeyword())
				&& !containsIgnoreCase(job.getSkills(), alert.getKeyword())) {
			return false;
		}
		if (StringUtils.isNotBlank(alert.getLocation()) && !containsIgnoreCase(job.getJobLocation(), alert.getLocation())) {
			return false;
		}
		if (StringUtils.isNotBlank(alert.getCategory()) && !containsIgnoreCase(job.getCategory(), alert.getCategory())) {
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
