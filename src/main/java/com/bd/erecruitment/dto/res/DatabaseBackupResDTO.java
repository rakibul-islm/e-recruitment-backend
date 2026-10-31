package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.DatabaseBackup;
import com.bd.erecruitment.enums.BackupStatus;
import com.bd.erecruitment.util.ModelMapperUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DatabaseBackupResDTO extends BaseResponseDTO<DatabaseBackup> {

	private String fileName;
	private BackupStatus status;
	private Long sizeBytes;
	private String sha256;
	private Integer tableCount;
	private Long rowCount;
	private String databaseProduct;
	private String errorMessage;
	private String driveStatus;
	private String driveError;
	private String createdBy;
	private Date createdOn;
	private Date finishedOn;

	public DatabaseBackupResDTO(DatabaseBackup backup) {
		ModelMapperUtils.MAPPER.map(backup, this);
	}
}
