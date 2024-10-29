package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.JobCircular;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class JobCircularReqDto extends BaseRequestDTO<JobCircular> {

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

	@JsonIgnore
	@Override
	public JobCircular getBean() {
		JobCircular u = new JobCircular();
		BeanUtils.copyProperties(this, u);
		return u;
	}

}
