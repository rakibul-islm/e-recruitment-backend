package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.JobCircular;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JobCircularResDTO extends BaseResponseDTO<JobCircular>{

	public JobCircularResDTO(JobCircular jobCircular){
		ModelMapperUtils.MAPPER.map(jobCircular, this);
	}

	private String jobTitle;
	private String organizationName;
	private String organizationAddress;
	private String organizationPhone;
	private String organizationEmail;
	private String organizationWebsite;
	private String organizationBusiness;
	private Long organizationId;
	@Temporal(TemporalType.DATE)
	private Date applicationDeadLine;
	private Integer vacancy;
	private String experience;
	private String salary;
	private Integer salaryMin;
	private Integer salaryMax;
	private String jobLocation;
	private String jobRequirement;
	private String jobResponsibilities;
	private String otherBenefits;
	private String workPlace;
	private String employmentStatus;
	private String skills;
	private String category;
	private String status;
}
