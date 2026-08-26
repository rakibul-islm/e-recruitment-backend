package com.bd.erecruitment.seed.data;

import java.util.List;

public class ArchiveConfigData {

	public record ConfigDef(String sourceTable, String archiveSchema, String archiveTable, String dateColumn, int retentionDays, String description) {}

	public static List<ConfigDef> get() {
		return List.of(
			new ConfigDef(
				"AUDIT_LOG", "archive", "AUDIT_LOG", "created_on", 365,
				"Audit trail rows past retention"
			),
			new ConfigDef(
				"EXCEPTION_LOG", "archive", "EXCEPTION_LOG", "created_on", 90,
				"Handled-exception rows past retention"
			),
			new ConfigDef(
				"USER_SESSION", "archive", "USER_SESSION", "expires_at", 90,
				"Sessions expired for longer than retention - never touches a still-valid session"
			)
		);
	}
}
