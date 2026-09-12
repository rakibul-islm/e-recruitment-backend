package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.McqTest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McqTestReqDto extends BaseRequestDTO<McqTest> {

	private String name;
	private String description;
	private Integer durationMinutes;
	private Integer passingScorePercent;
	private Integer questionSelectionCount;
	private Integer secondsPerQuestion;
	private boolean shuffleQuestions;
	private boolean shuffleOptions;
	private String status;
	private List<Long> questionIds = new ArrayList<>();

	@JsonIgnore
	@Override
	public McqTest getBean() {
		McqTest bean = new McqTest()
			.setName(name)
			.setDescription(description)
			.setDurationMinutes(durationMinutes)
			.setPassingScorePercent(passingScorePercent)
			.setQuestionSelectionCount(questionSelectionCount)
			.setSecondsPerQuestion(secondsPerQuestion)
			.setShuffleQuestions(shuffleQuestions)
			.setShuffleOptions(shuffleOptions)
			.setStatus(status);
		bean.getQuestionIds().addAll(questionIds);
		return bean;
	}
}
