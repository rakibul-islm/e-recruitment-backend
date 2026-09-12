package com.bd.erecruitment.dto.req;

import lombok.Data;

@Data
// optionKey is deliberately not settable here - it's always derived from list position
// (A/B/C/D...) server-side, in McqQuestionReqDto.getBean() and McqQuestionServiceImpl.update(),
// so it can never come back null/inconsistent regardless of what the frontend sends.
public class McqOptionReqDto {

	private String optionText;
	private boolean correct;
}
