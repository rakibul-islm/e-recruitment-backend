package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Field names must match job-posting-report.jrxml <field> declarations exactly.
@Getter
@AllArgsConstructor
public class JobPostingReportRow {

	private final String jobTitle;
	private final String organizationName;
	private final String status;
	private final Integer vacancy;
	private final String applicationDeadLine;
	private final String jobLocation;
	private final String employmentStatus;
	private final Integer applicantCount;
}
