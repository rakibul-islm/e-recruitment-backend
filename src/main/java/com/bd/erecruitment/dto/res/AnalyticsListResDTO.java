package com.bd.erecruitment.dto.res;

import lombok.Data;

import java.util.Date;

@Data
public class AnalyticsListResDTO {

	private Long applicationId;
	private Long jobId;
	private String jobTitle;
	private String candidateName;
	private String candidateEmail;
	private String status;
	private Date appliedOn;
}
