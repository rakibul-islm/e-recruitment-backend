package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.JobCircular;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class JobCircularResDTO extends BaseResponseDTO<JobCircular>{

	public JobCircularResDTO(JobCircular jobCircular){
		new ModelMapper().map(jobCircular, this);
	}

	private String jobTitle;
	private String companyName;
	private String companyAddress;
	private String companyPhone;
	private String companyEmail;
	private String companyWebsite;
	private String companyBusiness;
	@Temporal(TemporalType.DATE)
	private Date applicationDeadLine;
	private Integer vacancy;
	private String experience;
	private String salary;
	private String jobLocation;
	private String jobRequirement;
	private String jobResponsibilities;
	private String otherBenefits;
	private String workPlace;
	private String employmentStatus;
}
