package com.bd.erecruitment.dto.req;

import lombok.Data;

@Data
public class McqGenerateQuestionsReqDto {

	private String skillTag;
	private String difficulty;
	private Integer count;
}
