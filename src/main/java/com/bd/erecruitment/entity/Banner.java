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
@Table(name = "BANNER")
@EqualsAndHashCode(callSuper = true)
public class Banner extends SequenceIdGenerator {

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "file_id")
	private Long fileId;

	@Column(nullable = false)
	private boolean active;

	// Optional special-day window; null is unbounded.
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "visible_from")
	private Date visibleFrom;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "visible_to")
	private Date visibleTo;
}
