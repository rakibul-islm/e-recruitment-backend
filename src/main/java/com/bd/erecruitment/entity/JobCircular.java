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
	@Column(name = "company_name")
	private String organizationName;
	@Column(name = "company_address")
	private String organizationAddress;
	@Column(name = "company_phone")
	private String organizationPhone;
	@Column(name = "company_email")
	private String organizationEmail;
	@Column(name = "company_website")
	private String organizationWebsite;
	@Column(name = "company_business")
	private String organizationBusiness;

	@Column(name = "company_id")
	private Long organizationId;

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
