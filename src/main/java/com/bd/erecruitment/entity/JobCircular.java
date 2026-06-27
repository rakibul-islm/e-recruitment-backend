package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "JOB_CIRCULAR")
@EqualsAndHashCode(callSuper = true)
public class JobCircular extends SequenceIdGenerator{

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
