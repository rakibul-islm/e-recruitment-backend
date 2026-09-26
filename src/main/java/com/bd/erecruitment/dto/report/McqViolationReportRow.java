package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

// Field names must match mcq-violation-report.jrxml <field> declarations exactly.
@Getter
@AllArgsConstructor
public class McqViolationReportRow {

	private final String testName;
	private final String candidateName;
	private final String candidateEmail;
	private final String jobTitle;
	private final String violationType;
	private final String detail;
	private final Integer questionNumber;
	private final Integer sequenceNo;
	private final String action;
	private final Date occurredOn;
}
