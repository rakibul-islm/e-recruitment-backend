package com.bd.erecruitment.dto.res;

import lombok.Data;

import java.util.Map;

@Data
public class ApplicationFunnelResDTO {

	private Long jobCircularId;
	private String jobTitle;
	private long totalApplications;
	private Map<String, Long> statusCounts;
}
