package com.bd.erecruitment.dto.res;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class CandidateDashboardResDTO {

	private long totalApplications;
	private long activeApplications;
	private long offersToRespond;
	private long savedJobs;
	private long jobAlerts;
	private List<RecentApplication> recentApplications;

	@Data
	public static class RecentApplication {
		private Long id;
		private String jobTitle;
		private String status;
		private Date appliedOn;
	}
}
