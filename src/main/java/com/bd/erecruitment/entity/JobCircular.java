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

	// References Company.id - no JPA relation, same convention as User.userGroupId. Free-text
	// company* fields above are kept for backward compatibility / circulars with no linked Company.
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

	// Rich text (HTML) from the job posting form's editor - long enough for formatted lists, not just plain sentences.
	@Column(length = 4000)
	private String jobRequirement;

	@Column(length = 4000)
	private String jobResponsibilities;

	private String otherBenefits;
	private String workPlace;
	private String employmentStatus;

	// Comma-separated tags/skills, e.g. "java,spring boot,sql". Simple free-text list, no separate table.
	@Column(length = 500)
	private String skills;

	private String category;

	// DRAFT | PUBLISHED | CLOSED | EXPIRED - plain String, same convention as employmentStatus/workPlace.
	@Column(nullable = false, length = 20)
	@Builder.Default
	private String status = "DRAFT";
}
