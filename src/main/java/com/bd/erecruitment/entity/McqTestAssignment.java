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
@Table(name = "MCQ_TEST_ASSIGNMENT", indexes = {
	@Index(name = "idx_mcq_assign_deadline", columnList = "status, deleted, deadline_at"),
	@Index(name = "idx_mcq_assign_sched_end", columnList = "status, deleted, scheduled_end_at"),
	@Index(name = "idx_mcq_assign_q_deadline", columnList = "status, deleted, current_question_deadline_at")
})
@EqualsAndHashCode(callSuper = true)
public class McqTestAssignment extends SequenceIdGenerator {

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	@Column(name = "mcq_test_id", nullable = false)
	private Long mcqTestId;

	@Column(name = "candidate_user_id", nullable = false)
	private Long candidateUserId;

	@Column(nullable = false, length = 20)
	private String status;

	@Temporal(TemporalType.TIMESTAMP)
	private Date assignedOn;

	private String assignedBy;

	@Temporal(TemporalType.TIMESTAMP)
	private Date scheduledAt;

	@Column(name = "scheduled_end_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date scheduledEndAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date startedOn;

	@Column(name = "deadline_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date deadlineAt;

	@Column(name = "current_question_index")
	private Integer currentQuestionIndex;

	@Column(name = "seconds_per_question_snapshot")
	private Integer secondsPerQuestionSnapshot;

	@Column(name = "current_question_deadline_at")
	@Temporal(TemporalType.TIMESTAMP)
	private Date currentQuestionDeadlineAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date submittedOn;

	@Column(name = "submitted_via", length = 10)
	private String submittedVia;

	@Column(name = "duration_minutes_snapshot")
	private Integer durationMinutesSnapshot;

	@Column(name = "passing_score_percent_snapshot")
	private Integer passingScorePercentSnapshot;

	@Column(name = "score_percent")
	private Integer scorePercent;

	@Column(name = "correct_count")
	private Integer correctCount;

	@Column(name = "total_count")
	private Integer totalCount;

	private Boolean passed;
}
