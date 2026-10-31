package com.bd.erecruitment.seed.data;

import java.util.List;

public class SystemConfigData {

	public record ConfigDef(String key, String value, String description, String expectedValues) {}

	public static List<ConfigDef> get() {
		return List.of(
			new ConfigDef(
				"DB_BACKUP_SCHEDULE_ENABLED",
				"Y",
				"When Y, the database is backed up automatically on the schedule in DB_BACKUP_CRON",
				"Y,N"
			),
			new ConfigDef(
				"DB_BACKUP_CRON",
				"0 0 2 * * *",
				"When scheduled database backups run, as a 6-field cron expression (second minute hour day month weekday) in Asia/Dhaka time; the default is every day at 02:00",
				null
			),
			new ConfigDef(
				"DB_BACKUP_KEEP_COUNT",
				"7",
				"Number of scheduled database backups kept; older scheduled backups are deleted automatically (manual backups are never deleted)",
				null
			),
			new ConfigDef(
				"DB_BACKUP_DRIVE_ENABLED",
				"Y",
				"When Y, every manual and scheduled database backup is also uploaded to Google Drive (needs the DRIVE_REFRESH_TOKEN environment variable); deleting a backup here also deletes its Drive copy",
				"Y,N"
			),
			new ConfigDef(
				"EXCEPTION_LOG_TO_DB",
				"Y",
				"When Y, exceptions handled by GlobalExceptionHandler are also persisted to the exception_log table",
				"Y,N"
			),
			new ConfigDef(
				"AUDIT_LOG_ENABLED",
				"Y",
				"When Y, entity create/update/delete actions are persisted to the audit_log table",
				"Y,N"
			),
			new ConfigDef(
				"AUDIT_LOG_RETENTION_DAYS",
				"365",
				"Number of days audit_log rows are retained before the scheduled retention job purges them",
				null
			),
			new ConfigDef(
				"EXCEPTION_LOG_RETENTION_DAYS",
				"90",
				"Number of days exception_log rows are retained before the scheduled retention job archives and purges them",
				null
			),
			new ConfigDef(
				"USER_SESSION_RETENTION_DAYS",
				"90",
				"Number of days past expiry a user_session row is retained before the scheduled retention job archives and purges it",
				null
			),
			new ConfigDef(
				"MCQ_VIOLATION_MONITORING_ENABLED",
				"Y",
				"When Y, suspicious behavior during an MCQ test (tab switch, window blur, leaving fullscreen, copy attempts, blocked shortcuts) is recorded and the candidate is warned",
				"Y,N"
			),
			new ConfigDef(
				"MCQ_VIOLATION_LIMIT",
				"3",
				"Number of recorded violations a candidate may accumulate in one MCQ test; the violation after this number ends the test, auto-submits it and logs the candidate out",
				null
			)
		);
	}
}
