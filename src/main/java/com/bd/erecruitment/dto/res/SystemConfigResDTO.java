package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.SystemConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemConfigResDTO extends BaseResponseDTO<SystemConfig> {

	private String configKey;
	private String configValue;
	private String description;
	private String expectedValues;

	public SystemConfigResDTO(SystemConfig config) {
		new ModelMapper().map(config, this);
	}
}
