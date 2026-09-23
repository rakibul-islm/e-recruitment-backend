package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Interview;
import com.bd.erecruitment.entity.InterviewFeedbackItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class InterviewResDTO extends BaseResponseDTO<Interview> {

	public InterviewResDTO(Interview interview) {
		ModelMapperUtils.MAPPER.map(interview, this);
	}

	private Long applicationId;
	private String title;
	private Date scheduledAt;
	private Integer durationMinutes;
	private String mode;
	private String location;
	private String status;
	private List<Long> interviewerUserIds = new ArrayList<>();
	private List<InterviewFeedbackItem> feedback = new ArrayList<>();

	// Denormalized, populated by InterviewServiceImpl.
	private String jobTitle;
	private String candidateName;
}
