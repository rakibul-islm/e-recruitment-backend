package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

// Field names must match mcq-result-report.jrxml's <field> declarations exactly.
@Getter
@AllArgsConstructor
public class McqResultReportRow {

	private final String testName;
	private final String candidateName;
	private final String candidateEmail;
	private final String jobTitle;
	private final Integer scorePercent;
	private final String passed;
	private final Date submittedOn;
}
