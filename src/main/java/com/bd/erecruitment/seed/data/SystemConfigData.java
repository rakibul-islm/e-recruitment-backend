package com.bd.erecruitment.seed.data;

import java.util.List;

public class SystemConfigData {

	public record ConfigDef(String key, String value, String description, String expectedValues) {}

	public static List<ConfigDef> get() {
		return List.of(
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
