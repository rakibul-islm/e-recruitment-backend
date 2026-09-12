package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

// Field names must match audit-log-report.jrxml's <field> declarations exactly.
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
}
