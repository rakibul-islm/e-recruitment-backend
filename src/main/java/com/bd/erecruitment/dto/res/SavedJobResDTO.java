package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.SavedJob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SavedJobResDTO extends BaseResponseDTO<SavedJob> {

	public SavedJobResDTO(SavedJob savedJob) {
		ModelMapperUtils.MAPPER.map(savedJob, this);
	}

	private Long jobCircularId;
	private Date savedOn;

	// Denormalized, populated by SavedJobServiceImpl.
	private String jobTitle;
	private String companyName;
	private String jobStatus;
}
