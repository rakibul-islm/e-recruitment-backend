package com.bd.erecruitment.dto.req;

import lombok.Data;

@Data
public class SubmitAnswerReqDto {

	private Long assignmentQuestionId;
	private String selectedOptionKey;
}
