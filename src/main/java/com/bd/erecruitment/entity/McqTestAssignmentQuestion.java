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
