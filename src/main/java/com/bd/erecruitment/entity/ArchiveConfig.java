package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

// One row of runtime-editable retention policy for GenericArchiveEngine/ArchiveScheduler - identifiers and a day count only, no SQL predicate.
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

	// Always a separate schema from the source table's - archiving never targets the same schema.
	@Column(name = "archive_schema", nullable = false, length = 100)
	private String archiveSchema;

	@Column(name = "archive_table", nullable = false, length = 100)
	private String archiveTable;

	// Blank/null defaults to "created_on" at run time (see GenericArchiveEngine).
	@Column(name = "date_column", length = 100)
	private String dateColumn;

	@Column(name = "retention_days", nullable = false)
	private int retentionDays;

	@Column(name = "enabled", nullable = false)
	private boolean enabled;

	private String description;

	// Optional extra SQL boolean expression, ANDed onto the date-column threshold (see GenericArchiveEngine).
	@Column(name = "where_condition", length = 500)
	private String whereCondition;
}
