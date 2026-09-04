package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.ApplicationStatusHistory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ApplicationStatusHistoryResDTO extends BaseResponseDTO<ApplicationStatusHistory> {

	public ApplicationStatusHistoryResDTO(ApplicationStatusHistory history) {
		new ModelMapper().map(history, this);
	}

	private Long applicationId;
	private String status;
	private String note;
	private String changedBy;
	private Date changedOn;
}
