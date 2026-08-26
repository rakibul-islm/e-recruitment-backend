package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

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
		new ModelMapper().map(this, c);
		return c;
	}
}
