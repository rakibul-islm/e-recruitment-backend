package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.SystemConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SystemConfigReqDto extends BaseRequestDTO<SystemConfig> {

	private String configKey;
	private String configValue;
	private String description;
	private String expectedValues;

	@JsonIgnore
	@Override
	public SystemConfig getBean() {
		SystemConfig c = new SystemConfig();
		new ModelMapper().map(this, c);
		return c;
	}
}
