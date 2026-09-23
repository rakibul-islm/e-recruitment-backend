package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.JobAlert;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JobAlertResDTO extends BaseResponseDTO<JobAlert> {

	public JobAlertResDTO(JobAlert jobAlert) {
		ModelMapperUtils.MAPPER.map(jobAlert, this);
	}

	private String keyword;
	private String location;
	private String category;
	private boolean active;
	private Date lastNotifiedOn;
}
