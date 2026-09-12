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

// A reusable bank question. status: DRAFT|APPROVED - only APPROVED questions may be pooled into
// an McqTest. source: MANUAL|AI_GENERATED - AI drafts always land DRAFT, pending recruiter review.
// companyId scopes the bank per-company, same convention as JobCircular.companyId.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "MCQ_QUESTION")
@EqualsAndHashCode(callSuper = true)
public class McqQuestion extends SequenceIdGenerator {

	@Column(name = "company_id")
	private Long companyId;

	@Column(name = "question_text", nullable = false, length = 2000)
	private String questionText;

	@Column(name = "skill_tag", length = 100)
	private String skillTag;

	@Column(nullable = false, length = 20)
	private String difficulty;

	@Column(nullable = false, length = 20)
	private String status;

	@Column(nullable = false, length = 20)
	private String source;

	@Column(length = 2000)
	private String explanation;

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "MCQ_QUESTION_OPTION", joinColumns = @JoinColumn(name = "question_id"))
	private List<McqOptionItem> options = new ArrayList<>();
}
