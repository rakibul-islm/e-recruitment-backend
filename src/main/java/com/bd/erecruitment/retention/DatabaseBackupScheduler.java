package com.bd.erecruitment.retention;

import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.service.impl.DatabaseBackupServiceImpl;
import com.bd.erecruitment.service.impl.SystemConfigServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Component
public class DatabaseBackupScheduler {

	private static final ZoneId ZONE = ZoneId.of("Asia/Dhaka");
	private static final int DEFAULT_KEEP_COUNT = 7;

	private final DatabaseBackupServiceImpl backupService;
	private final SystemConfigServiceImpl systemConfigService;
	private final ZonedDateTime startedAt = ZonedDateTime.now(ZONE);
	private String lastInvalidCron;

	public DatabaseBackupScheduler(DatabaseBackupServiceImpl backupService, SystemConfigServiceImpl systemConfigService) {
		this.backupService = backupService;
		this.systemConfigService = systemConfigService;
	}

	// Ticks every minute and reads the config each time, so a changed schedule applies without a restart.
	@Scheduled(cron = "0 * * * * *", zone = "Asia/Dhaka")
	public void runDueBackup() {
		if (!"Y".equalsIgnoreCase(configValue("DB_BACKUP_SCHEDULE_ENABLED"))) return;

		CronExpression cron = parseCron(configValue("DB_BACKUP_CRON"));
		if (cron == null) return;

		ZonedDateTime lastRun = backupService.lastScheduledAt().map(d -> d.toInstant().atZone(ZONE)).orElse(startedAt);
		ZonedDateTime due = cron.next(lastRun);
		if (due == null || due.isAfter(ZonedDateTime.now(ZONE))) return;

		log.info("[DatabaseBackupScheduler] starting scheduled backup (due {})", due);
		backupService.startScheduled(keepCount());
	}

	private CronExpression parseCron(String expression) {
		try {
			return CronExpression.parse(expression == null ? "" : expression.trim());
		} catch (IllegalArgumentException e) {
			if (!StringUtils.equals(lastInvalidCron, expression)) {
				log.error("[DatabaseBackupScheduler] DB_BACKUP_CRON '{}' is not a valid 6-field cron expression; scheduled backups are paused", expression);
				lastInvalidCron = expression;
			}
			return null;
		}
	}

	private int keepCount() {
		try {
			return Integer.parseInt(configValue("DB_BACKUP_KEEP_COUNT").trim());
		} catch (RuntimeException e) {
			return DEFAULT_KEEP_COUNT;
		}
	}

	private String configValue(String key) {
		SystemConfig config = systemConfigService.findCachedByKey(key);
		return config == null ? null : config.getConfigValue();
	}
}
