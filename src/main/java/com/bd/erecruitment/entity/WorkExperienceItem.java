package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Embeddable
@NoArgsConstructor
public class WorkExperienceItem {

	private String title;
	private String companyName;
	private String location;

	@Temporal(TemporalType.DATE)
	private Date startDate;

	@Temporal(TemporalType.DATE)
	private Date endDate;

	private boolean current;

	@Column(length = 2000)
	private String description;
}
