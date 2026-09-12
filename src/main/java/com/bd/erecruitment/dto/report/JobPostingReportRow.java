package com.bd.erecruitment.dto.report;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Date;

// Field names must match job-posting-report.jrxml's <field> declarations exactly.
@Getter
@AllArgsConstructor
public class JobPostingReportRow {

	private final String jobTitle;
	private final String companyName;
	private final String status;
	private final Integer vacancy;
	private final Date applicationDeadLine;
	private final String jobLocation;
	private final String employmentStatus;
	private final Integer applicantCount;
}
