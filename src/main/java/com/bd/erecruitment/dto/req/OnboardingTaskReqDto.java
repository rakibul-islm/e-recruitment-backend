package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.Date;

@Data
public class OnboardingTaskReqDto {

	private Long applicationId;
	private String title;
	private String description;
	private Date dueDate;
}
