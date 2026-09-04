package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Generic blob storage (resumes, generated CVs, offer letters, ...). DB-backed for now via
// StorageService - see StorageService for why, and how to swap in an object-storage backend later.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "STORED_FILE")
@EqualsAndHashCode(callSuper = true)
public class StoredFile extends SequenceIdGenerator {

	@Column(nullable = false, length = 255)
	private String filename;

	@Column(name = "content_type", nullable = false, length = 100)
	private String contentType;

	@Column(nullable = false)
	private long size;

	@JdbcTypeCode(SqlTypes.VARBINARY)
	@Column(name = "data", nullable = false, length = 20_000_000)
	private byte[] data;
}
