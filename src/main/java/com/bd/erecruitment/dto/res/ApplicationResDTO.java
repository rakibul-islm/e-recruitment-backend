package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.Application;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApplicationResDTO extends BaseResponseDTO<Application> {

	public ApplicationResDTO(Application application) {
		new ModelMapper().map(application, this);
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

	// Denormalized display fields, populated by ApplicationServiceImpl - avoids a second round
	// trip from the frontend for the job title / candidate name shown on list & detail screens.
	private String jobTitle;
	private String candidateName;
	private String candidateEmail;
}
