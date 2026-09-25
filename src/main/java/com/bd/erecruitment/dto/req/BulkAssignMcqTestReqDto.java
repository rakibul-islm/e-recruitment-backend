package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class BulkAssignMcqTestReqDto {

	private List<Long> applicationIds;
	private Long mcqTestId;

	private Date scheduledAt;
	private Date scheduledEndAt;
}
