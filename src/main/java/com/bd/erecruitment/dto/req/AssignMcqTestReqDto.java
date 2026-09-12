package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.Date;

@Data
public class AssignMcqTestReqDto {

	private Long applicationId;
	private Long mcqTestId;

	// Both optional - see McqTestAssignment.scheduledAt/scheduledEndAt.
	private Date scheduledAt;
	private Date scheduledEndAt;
}
