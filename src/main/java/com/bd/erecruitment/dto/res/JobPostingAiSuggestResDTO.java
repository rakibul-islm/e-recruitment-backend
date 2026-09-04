package com.bd.erecruitment.dto.res;

import lombok.Data;

@Data
public class JobPostingAiSuggestResDTO {

	private String jobRequirement;
	private String jobResponsibilities;
	private String otherBenefits;
	private String skills;
	private String category;
	private String experience;
	private String employmentStatus;
}
