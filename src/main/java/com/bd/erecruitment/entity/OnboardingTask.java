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

// A single post-hire checklist item for an Application. Seeded with a default set on offer
// acceptance (see OnboardingServiceImpl.seedDefaultTasks), and staff can add custom ones.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "ONBOARDING_TASK")
@EqualsAndHashCode(callSuper = true)
public class OnboardingTask extends SequenceIdGenerator {

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(length = 1000)
	private String description;

	@Temporal(TemporalType.DATE)
	private Date dueDate;

	@Column(nullable = false)
	private boolean completed;

	@Temporal(TemporalType.TIMESTAMP)
	private Date completedOn;

	private String completedBy;
}
