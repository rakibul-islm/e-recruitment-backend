package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Application;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApplicationResDTO extends BaseResponseDTO<Application> {

	public ApplicationResDTO(Application application) {
		ModelMapperUtils.MAPPER.map(application, this);
	}

	private Long jobCircularId;
	private Long candidateUserId;
	private String status;
	private String coverLetter;
	private Long resumeFileId;
	private Long generatedCvId;
	private Date appliedOn;
	private Date statusUpdatedOn;
	private String statusUpdatedBy;

	private String jobTitle;
	private String candidateName;
	private String candidateEmail;
}
