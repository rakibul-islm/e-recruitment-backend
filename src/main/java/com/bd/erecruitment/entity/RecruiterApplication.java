package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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

	@Column(nullable = false, length = 200)
	private String companyName;

	private String companyWebsite;
	private String companyIndustry;
	private String companySize;
	private String companyAddress;
	private String companyPhone;
	private String companyEmail;

	@Column(length = 2000)
	private String companyDescription;

	private String jobTitle;

	@Column(length = 2000)
	private String message;

	@Column(nullable = false, length = 20)
	private String status = "PENDING";

	@Column(length = 500)
	private String reviewNote;
}
