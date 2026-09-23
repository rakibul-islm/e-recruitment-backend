package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArchiveConfigReqDto extends BaseRequestDTO<ArchiveConfig> {

	private String sourceTable;
	private String archiveSchema;
	private String archiveTable;
	private String dateColumn;
	private int retentionDays;
	private boolean enabled;
	private String description;
	private String whereCondition;

	@JsonIgnore
	@Override
	public ArchiveConfig getBean() {
		ArchiveConfig c = new ArchiveConfig();
		ModelMapperUtils.MAPPER.map(this, c);
		return c;
	}
}
