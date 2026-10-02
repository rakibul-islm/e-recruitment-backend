package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

// Field names must match audit-log-report.jrxml <field> declarations exactly.
@Getter
@AllArgsConstructor
public class AuditLogReportRow {

	private final String category;
	private final String action;
	private final String entityType;
	private final Long entityId;
	private final String outcome;
	private final String createdBy;
	private final Date createdOn;
	private final String ipAddress;
	private final String city;
	private final String country;
	private final String deviceType;
	private final String os;
	private final String browser;
}
