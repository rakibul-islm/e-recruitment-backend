package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.JobAlert;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JobAlertResDTO extends BaseResponseDTO<JobAlert> {

	public JobAlertResDTO(JobAlert jobAlert) {
		new ModelMapper().map(jobAlert, this);
	}

	private String keyword;
	private String location;
	private String category;
	private boolean active;
	private Date lastNotifiedOn;
}
