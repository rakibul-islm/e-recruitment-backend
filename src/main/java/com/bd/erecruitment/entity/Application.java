package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
@Table(name = "APPLICATION", indexes = {
	@Index(name = "idx_application_job", columnList = "job_circular_id"),
	@Index(name = "idx_application_candidate", columnList = "candidate_user_id"),
	@Index(name = "idx_application_status", columnList = "status"),
	@Index(name = "idx_application_applied_on", columnList = "applied_on")
})
@EqualsAndHashCode(callSuper = true)
public class Application extends SequenceIdGenerator {

	@Column(name = "job_circular_id", nullable = false)
	private Long jobCircularId;

	@Column(name = "candidate_user_id", nullable = false)
	private Long candidateUserId;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(length = 4000)
	private String coverLetter;

	@Column(name = "resume_file_id")
	private Long resumeFileId;

	@Column(name = "generated_cv_id")
	private Long generatedCvId;

	@Temporal(TemporalType.TIMESTAMP)
	private Date appliedOn;

	@Temporal(TemporalType.TIMESTAMP)
	private Date statusUpdatedOn;

	private String statusUpdatedBy;

	@Column(name = "match_score")
	private Integer matchScore;
}
