package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.McqTest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McqTestResDTO extends BaseResponseDTO<McqTest> {

	public McqTestResDTO(McqTest test) {
		new ModelMapper().map(test, this);
		this.questionCount = test.getQuestionIds() != null ? test.getQuestionIds().size() : 0;
	}

	private Long companyId;
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

	// Denormalized, populated from McqTest.questionIds.size() by the constructor above.
	private int questionCount;
}
