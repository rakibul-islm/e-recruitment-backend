package com.bd.erecruitment.dto.res;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Candidate-facing view of one assigned question. Deliberately never carries option correctness,
// even after submission - only the aggregate score/pass on McqTestAssignmentResDTO is shown to
// the candidate, so answers can't leak to later test-takers.
@Data
@NoArgsConstructor
public class McqTestAttemptQuestionDto {

	private Long id;
	private int displayOrder;
	private String questionText;
	private String selectedOptionKey;
	private List<Option> options = new ArrayList<>();

	@Data
	@NoArgsConstructor
	public static class Option {
		private String optionKey;
		private String optionText;
		private int displayOrder;
	}
}
