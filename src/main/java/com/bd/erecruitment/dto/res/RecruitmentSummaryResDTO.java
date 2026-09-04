package com.bd.erecruitment.dto.res;

import lombok.Data;

import java.util.Map;

@Data
public class RecruitmentSummaryResDTO {

	private long totalJobs;
	private long publishedJobs;
	private long totalApplications;
	private long applicationsLast30Days;
	private long hiresLast30Days;
	private Double avgTimeToHireDays;
	private Map<String, Long> applicationsByStatus;
}
