package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "RECRUITER_APPLICATION")
@EqualsAndHashCode(callSuper = true)
public class RecruiterApplication extends SequenceIdGenerator {

	@Column(nullable = false, length = 150)
	private String fullName;

	@Column(nullable = false, length = 150)
	private String email;

	private String phone;

	@Column(name = "company_name", nullable = false, length = 200)
	private String organizationName;

	@Column(name = "company_website")
	private String organizationWebsite;
	@Column(name = "company_industry")
	private String organizationType;
	@Column(name = "company_size")
	private String organizationSize;
	@Column(name = "company_address")
	private String organizationAddress;
	@Column(name = "company_phone")
	private String organizationPhone;
	@Column(name = "company_email")
	private String organizationEmail;

	@Column(name = "company_description", length = 2000)
	private String organizationDescription;

	private String jobTitle;

	@Column(length = 2000)
	private String message;

	@Column(nullable = false, length = 20)
	@Builder.Default
	private String status = "PENDING";

	@Column(length = 500)
	private String reviewNote;
}
