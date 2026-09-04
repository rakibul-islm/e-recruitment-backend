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
@Table(name = "APPLICATION_STATUS_HISTORY")
@EqualsAndHashCode(callSuper = true)
public class ApplicationStatusHistory extends SequenceIdGenerator {

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(length = 1000)
	private String note;

	private String changedBy;

	@Temporal(TemporalType.TIMESTAMP)
	private Date changedOn;
}
