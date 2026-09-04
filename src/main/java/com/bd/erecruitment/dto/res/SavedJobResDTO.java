package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.SavedJob;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SavedJobResDTO extends BaseResponseDTO<SavedJob> {

	public SavedJobResDTO(SavedJob savedJob) {
		new ModelMapper().map(savedJob, this);
	}

	private Long jobCircularId;
	private Date savedOn;

	// Denormalized, populated by SavedJobServiceImpl.
	private String jobTitle;
	private String companyName;
	private String jobStatus;
}
