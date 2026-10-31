package com.bd.erecruitment.entity;

import com.bd.erecruitment.enums.BackupStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "DATABASE_BACKUP")
@EqualsAndHashCode(callSuper = true)
public class DatabaseBackup extends SequenceIdGenerator {

	@Column(name = "file_name", nullable = false, length = 150)
	private String fileName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BackupStatus status;

	@Column(name = "size_bytes")
	private Long sizeBytes;

	@Column(length = 64)
	private String sha256;

	@Column(name = "table_count")
	private Integer tableCount;

	@Column(name = "row_count")
	private Long rowCount;

	@Column(name = "database_product", length = 100)
	private String databaseProduct;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "finished_on")
	private Date finishedOn;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "drive_file_id", length = 100)
	private String driveFileId;

	@Column(name = "drive_status", length = 20)
	private String driveStatus;

	@Column(name = "drive_error", length = 600)
	private String driveError;
}
