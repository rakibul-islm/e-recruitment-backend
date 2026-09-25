package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Builder;
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

	@Column(name = "company_id")
	private Long companyId;

	@Temporal(TemporalType.DATE)
	private Date applicationDeadLine;
	private Integer vacancy;
	private String experience;
	private String salary;
	private Integer salaryMin;
	private Integer salaryMax;
	private String jobLocation;

	@Column(length = 4000)
	private String jobRequirement;

	@Column(length = 4000)
	private String jobResponsibilities;

	@Column(length = 1000)
	private String otherBenefits;
	private String workPlace;
	private String employmentStatus;

	@Column(length = 500)
	private String skills;

	private String category;

	@Column(nullable = false, length = 20)
	@Builder.Default
	private String status = "DRAFT";

	// Set on each move into PUBLISHED, not on plain edits; null on legacy rows (scheduler falls back to createdOn).
	@Temporal(TemporalType.TIMESTAMP)
	private Date publishedOn;
}
