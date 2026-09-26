package com.bd.erecruitment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class McqViolationResDTO {

	private boolean counted;
	private int violationCount;
	private int limit;
	private String action;
}
