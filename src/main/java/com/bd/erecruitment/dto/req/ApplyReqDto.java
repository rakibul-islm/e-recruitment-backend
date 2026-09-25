package com.bd.erecruitment.dto.req;

import lombok.Data;

@Data
public class ApplyReqDto {

	private Long jobCircularId;
	private String coverLetter;

	private Boolean useLatestGeneratedCv = true;

	private Long resumeFileId;
}
