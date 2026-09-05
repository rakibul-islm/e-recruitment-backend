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

	// Boolean, not boolean: pre-existing rows have NULL here, which a primitive boolean can't hold.
	private Boolean current;

	private String grade;
}
