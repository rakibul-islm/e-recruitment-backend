package com.bd.erecruitment.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Embeddable
@NoArgsConstructor
public class EducationItem {

	private String institution;
	private String degree;
	private String fieldOfStudy;

	@Temporal(TemporalType.DATE)
	private Date startDate;

	@Temporal(TemporalType.DATE)
	private Date endDate;

	private String grade;
}
