package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.OnboardingTask;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OnboardingTaskResDTO extends BaseResponseDTO<OnboardingTask> {

	public OnboardingTaskResDTO(OnboardingTask task) {
		new ModelMapper().map(task, this);
	}

	private Long applicationId;
	private String title;
	private String description;
	private Date dueDate;
	private boolean completed;
	private Date completedOn;
	private String completedBy;
}
