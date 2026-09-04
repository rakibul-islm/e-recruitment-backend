package com.bd.erecruitment.dto.req;

import lombok.Data;

@Data
public class JobAlertReqDto {

	private Long id;
	private String keyword;
	private String location;
	private String category;
	private Boolean active = true;
}
