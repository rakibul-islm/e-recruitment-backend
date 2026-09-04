package com.bd.erecruitment.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class JobPostingAiSuggestReqDto {

	private String jobTitle;
	private String companyName;
	private String jobLocation;
	private String employmentStatus;
	private String experience;
	private String category;
	private String skills;
}
