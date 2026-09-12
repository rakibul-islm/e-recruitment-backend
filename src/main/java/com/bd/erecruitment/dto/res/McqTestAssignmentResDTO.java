package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.McqTestAssignment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

// Header-only view of an attempt - safe to return to both the candidate and staff. Never carries
// per-question detail (see McqTestAttemptQuestionDto / McqAssignmentQuestionReviewDto for that).
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class McqTestAssignmentResDTO extends BaseResponseDTO<McqTestAssignment> {

	public McqTestAssignmentResDTO(McqTestAssignment assignment) {
		new ModelMapper().map(assignment, this);
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

	// Denormalized, populated by McqTestAssignmentServiceImpl.
	private String testName;
	private String jobTitle;
	private String candidateName;
}
