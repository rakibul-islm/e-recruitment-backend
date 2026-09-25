package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.Date;

@Data
public class AssignMcqTestReqDto {

	private Long applicationId;
	private Long mcqTestId;

	private Date scheduledAt;
	private Date scheduledEndAt;
}
