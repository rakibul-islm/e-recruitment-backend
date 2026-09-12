package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

// Field names must match application-report.jrxml's <field> declarations exactly.
@Getter
@AllArgsConstructor
public class ApplicationReportRow {

	private final String jobTitle;
	private final String candidateName;
	private final String candidateEmail;
	private final String status;
	private final Date appliedOn;
}
