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

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "SAVED_JOB")
@EqualsAndHashCode(callSuper = true)
public class SavedJob extends SequenceIdGenerator {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "job_circular_id", nullable = false)
	private Long jobCircularId;

	@Temporal(TemporalType.TIMESTAMP)
	private Date savedOn;
}
