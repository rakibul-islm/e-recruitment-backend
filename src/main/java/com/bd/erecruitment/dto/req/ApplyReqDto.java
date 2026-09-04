package com.bd.erecruitment.dto.req;

import lombok.Data;

@Data
public class ApplyReqDto {

	private Long jobCircularId;
	private String coverLetter;

	// If true (default), the candidate's most recently generated CV is attached automatically.
	private Boolean useLatestGeneratedCv = true;

	// References StoredFile.id from a prior upload - alternative/addition to the generated CV.
	private Long resumeFileId;
}
