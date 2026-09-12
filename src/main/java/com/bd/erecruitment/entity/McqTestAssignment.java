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

// One candidate's attempt at an McqTest for one Application. status: ASSIGNED|IN_PROGRESS|
// SUBMITTED|EXPIRED. Selection/order is frozen at assign time (McqTestAssignmentQuestion rows
// created immediately); the timer only starts on the candidate's first /start call, stamping
// deadlineAt once so both the sweep query and the frontend countdown are trivial and stable
// across reloads. durationMinutesSnapshot/passingScorePercentSnapshot are copied from McqTest at
// assign time so a later test edit can't change rules mid-flight.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "MCQ_TEST_ASSIGNMENT")
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

	// Both optional and independent - "not before" / "not after" start gates for a scheduled exam,
	// set by the recruiter at assign time. Distinct from deadlineAt (below), which only governs how
	// long an already-started attempt runs. Null means "start any time after assignment", the
	// original (pre-scheduling) behavior.
	@Temporal(TemporalType.TIMESTAMP)
	private Date scheduledAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date scheduledEndAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date startedOn;

	@Temporal(TemporalType.TIMESTAMP)
	private Date deadlineAt;

	// Server-tracked "how far the candidate has advanced" - the actual forward-only enforcement for
	// the one-question-at-a-time exam flow. The frontend's own currentIndex is just UI state and
	// resets to 0 on page reload; without this, a reload would re-show (and let the candidate
	// re-answer) every earlier question. Only ever moves forward (see
	// McqTestAssignmentServiceImpl.advance()), and answer() rejects writes to any question row
	// whose displayOrder isn't exactly this value, so the lock holds even against a direct API call
	// bypassing the UI entirely.
	@Column(name = "current_question_index")
	private Integer currentQuestionIndex;

	// Optional per-question pacing, copied from McqTest.secondsPerQuestion at assign time - null
	// means this attempt has no per-question limit (only the whole-attempt deadlineAt applies).
	// currentQuestionDeadlineAt is (re)stamped every time currentQuestionIndex changes (on /start
	// for question 0, on /advance for each question after) so - same reasoning as
	// currentQuestionIndex above - a reload restores the real remaining time for the question the
	// candidate is on, rather than granting a fresh full window per reload.
	@Column(name = "seconds_per_question_snapshot")
	private Integer secondsPerQuestionSnapshot;

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
