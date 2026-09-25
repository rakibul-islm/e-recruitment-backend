package com.bd.erecruitment.dto.res;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

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
