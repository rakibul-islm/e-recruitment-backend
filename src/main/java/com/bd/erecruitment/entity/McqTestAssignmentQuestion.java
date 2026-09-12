package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// A frozen per-candidate snapshot of one McqQuestion, created at assignment time. Question text
// and options are copied in full (not read live from McqQuestion) so an in-progress or completed
// attempt is immune to the bank question later being edited/archived, and so there's a permanent
// disputable record of exactly what was shown and how it was graded. displayOrder is this
// candidate's shuffled position. selectedOptionKey/isCorrect/answeredOn are filled in as the
// candidate answers.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "MCQ_TEST_ASSIGNMENT_QUESTION")
@EqualsAndHashCode(callSuper = true)
public class McqTestAssignmentQuestion extends SequenceIdGenerator {

	@Column(name = "assignment_id", nullable = false)
	private Long assignmentId;

	// Traceability only - grading/rendering always uses the frozen snapshot below, never a live lookup.
	@Column(name = "question_id", nullable = false)
	private Long questionId;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "question_text_snapshot", nullable = false, length = 2000)
	private String questionTextSnapshot;

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "MCQ_ASSIGNMENT_QUESTION_OPTION", joinColumns = @JoinColumn(name = "assignment_question_id"))
	private List<McqAssignmentOptionItem> options = new ArrayList<>();

	@Column(name = "selected_option_key", length = 5)
	private String selectedOptionKey;

	@Column(name = "is_correct")
	private Boolean correct;

	@Temporal(TemporalType.TIMESTAMP)
	private Date answeredOn;
}
