package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BulkAssignMcqTestReqDto {

	private List<Long> applicationIds;
	private Long mcqTestId;

	// Both optional - see McqTestAssignment.scheduledAt/scheduledEndAt. Applied identically to
	// every application in the batch (one fixed exam date/time for the whole group).
	private Date scheduledAt;
	private Date scheduledEndAt;
}
