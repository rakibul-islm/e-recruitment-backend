package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Date;

// A candidate's saved search; JobAlertScheduler runs daily, emails a digest of jobs published
// since lastNotifiedOn matching keyword/location/category, and advances lastNotifiedOn.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "JOB_ALERT")
@EqualsAndHashCode(callSuper = true)
public class JobAlert extends SequenceIdGenerator {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	private String keyword;
	private String location;
	private String category;

	@Column(nullable = false)
	private boolean active;

	@Temporal(TemporalType.TIMESTAMP)
	private Date lastNotifiedOn;
}
