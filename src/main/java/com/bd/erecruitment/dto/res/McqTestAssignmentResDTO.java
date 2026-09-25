package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.McqTestAssignment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McqTestAssignmentResDTO extends BaseResponseDTO<McqTestAssignment> {

	public McqTestAssignmentResDTO(McqTestAssignment assignment) {
		ModelMapperUtils.MAPPER.map(assignment, this);
	}

	private Long applicationId;
	private Long mcqTestId;
	private String status;
	private Date assignedOn;
	private String assignedBy;
	private Date scheduledAt;
	private Date scheduledEndAt;
	private Date startedOn;
	private Date deadlineAt;
	private Integer currentQuestionIndex;
	private Integer secondsPerQuestionSnapshot;
	private Date currentQuestionDeadlineAt;
	private Date submittedOn;
	private String submittedVia;
	private Integer durationMinutesSnapshot;
	private Integer passingScorePercentSnapshot;
	private Integer scorePercent;
	private Integer correctCount;
	private Integer totalCount;
	private Boolean passed;

	private String testName;
	private String jobTitle;
	private String candidateName;
}
