package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.McqOptionItem;
import com.bd.erecruitment.entity.McqQuestion;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McqQuestionReqDto extends BaseRequestDTO<McqQuestion> {

	private String questionText;
	private String skillTag;
	private String difficulty;
	private String status;
	private String explanation;
	private List<McqOptionReqDto> options = new ArrayList<>();

	@JsonIgnore
	@Override
	public McqQuestion getBean() {
		McqQuestion bean = new McqQuestion()
			.setQuestionText(questionText)
			.setSkillTag(skillTag)
			.setDifficulty(difficulty)
			.setStatus(status)
			.setExplanation(explanation);
		int order = 0;
		for (McqOptionReqDto opt : options) {
			bean.getOptions().add(new McqOptionItem()
				.setOptionKey(String.valueOf((char) ('A' + order)))
				.setOptionText(opt.getOptionText())
				.setCorrect(opt.isCorrect())
				.setDisplayOrder(order++));
		}
		return bean;
	}
}
