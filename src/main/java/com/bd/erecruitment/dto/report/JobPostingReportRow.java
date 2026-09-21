package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Field names must match job-posting-report.jrxml's <field> declarations exactly.
@Getter
@AllArgsConstructor
public class JobPostingReportRow {

	private final String jobTitle;
	private final String companyName;
	private final String status;
	private final Integer vacancy;
	// Pre-formatted calendar date: a DATE column must not shift with the viewer's zone.
	private final String applicationDeadLine;
	private final String jobLocation;
	private final String employmentStatus;
	private final Integer applicantCount;
}
