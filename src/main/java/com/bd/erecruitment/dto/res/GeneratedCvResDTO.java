package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.GeneratedCv;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class GeneratedCvResDTO extends BaseResponseDTO<GeneratedCv> {

	public GeneratedCvResDTO(GeneratedCv cv) {
		new ModelMapper().map(cv, this);
	}

	private String templateKey;
	private Long storedFileId;
	private Date generatedOn;
}
