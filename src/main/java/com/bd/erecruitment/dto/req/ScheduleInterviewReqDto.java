package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ScheduleInterviewReqDto {

	private Long applicationId;
	private String title;
	private Date scheduledAt;
	private Integer durationMinutes;
	private String mode;
	private String location;
	private List<Long> interviewerUserIds;
}
