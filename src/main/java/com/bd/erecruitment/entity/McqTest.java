package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "MCQ_TEST")
@EqualsAndHashCode(callSuper = true)
public class McqTest extends SequenceIdGenerator {

	@Column(name = "company_id")
	private Long companyId;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(name = "duration_minutes", nullable = false)
	private Integer durationMinutes;

	@Column(name = "passing_score_percent", nullable = false)
	private Integer passingScorePercent;

	@Column(name = "question_selection_count")
	private Integer questionSelectionCount;

	@Column(name = "seconds_per_question")
	private Integer secondsPerQuestion;

	@Column(name = "shuffle_questions", nullable = false)
	private boolean shuffleQuestions;

	@Column(name = "shuffle_options", nullable = false)
	private boolean shuffleOptions;

	@Column(nullable = false, length = 20)
	private String status;

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "MCQ_TEST_QUESTION", joinColumns = @JoinColumn(name = "test_id"))
	@Column(name = "question_id")
	private List<Long> questionIds = new ArrayList<>();
}
