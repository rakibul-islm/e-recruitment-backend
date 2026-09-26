package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "MCQ_TEST_VIOLATION", indexes = {
	@Index(name = "idx_mcq_violation_assignment", columnList = "assignment_id, deleted"),
	@Index(name = "idx_mcq_violation_test", columnList = "mcq_test_id, deleted")
})
@EqualsAndHashCode(callSuper = true)
public class McqTestViolation extends SequenceIdGenerator {

	@Column(name = "assignment_id", nullable = false)
	private Long assignmentId;

	@Column(name = "mcq_test_id", nullable = false)
	private Long mcqTestId;

	@Column(name = "candidate_user_id", nullable = false)
	private Long candidateUserId;

	@Column(name = "violation_type", nullable = false, length = 30)
	private String violationType;

	@Column(length = 100)
	private String detail;

	@Column(name = "question_number")
	private Integer questionNumber;

	@Column(name = "sequence_no", nullable = false)
	private Integer sequenceNo;

	@Column(nullable = false, length = 20)
	private String action;

	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	@Column(name = "user_agent", length = 255)
	private String userAgent;
}
