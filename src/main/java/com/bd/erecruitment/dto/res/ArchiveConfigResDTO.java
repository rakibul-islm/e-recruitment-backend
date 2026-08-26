package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.ArchiveConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArchiveConfigResDTO extends BaseResponseDTO<ArchiveConfig> {

	private String sourceTable;
	private String archiveSchema;
	private String archiveTable;
	private String dateColumn;
	private int retentionDays;
	private boolean enabled;
	private String description;
	private String whereCondition;

	public ArchiveConfigResDTO(ArchiveConfig config) {
		new ModelMapper().map(config, this);
	}
}
