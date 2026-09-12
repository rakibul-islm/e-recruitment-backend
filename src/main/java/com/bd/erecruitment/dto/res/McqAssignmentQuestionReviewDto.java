package com.bd.erecruitment.dto.res;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

// Staff-facing view of one assigned question - full detail, including which option was correct
// and whether the candidate's answer matched. Never returned to the candidate.
@Data
@NoArgsConstructor
public class McqAssignmentQuestionReviewDto {

	private Long id;
	private int displayOrder;
	private String questionText;
	private String selectedOptionKey;
	private Boolean correct;
	private List<Option> options = new ArrayList<>();

	@Data
	@NoArgsConstructor
	public static class Option {
		private String optionKey;
		private String optionText;
		private boolean correct;
		private int displayOrder;
	}
}
