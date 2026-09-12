package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.McqOptionItem;
import com.bd.erecruitment.entity.McqQuestion;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McqQuestionResDTO extends BaseResponseDTO<McqQuestion> {

	public McqQuestionResDTO(McqQuestion question) {
		new ModelMapper().map(question, this);
	}

	private Long companyId;
	private String questionText;
	private String skillTag;
	private String difficulty;
	private String status;
	private String source;
	private String explanation;
	private List<McqOptionItem> options = new ArrayList<>();
}
