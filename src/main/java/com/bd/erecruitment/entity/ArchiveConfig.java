package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "ARCHIVE_CONFIG")
@EqualsAndHashCode(callSuper = true)
public class ArchiveConfig extends SequenceIdGenerator {

	@Column(name = "source_table", unique = true, nullable = false, length = 100)
	private String sourceTable;

	@Column(name = "archive_schema", nullable = false, length = 100)
	private String archiveSchema;

	@Column(name = "archive_table", nullable = false, length = 100)
	private String archiveTable;

	@Column(name = "date_column", length = 100)
	private String dateColumn;

	@Column(name = "retention_days", nullable = false)
	private int retentionDays;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	private String description;

	@Column(name = "where_condition", length = 500)
	private String whereCondition;
}
