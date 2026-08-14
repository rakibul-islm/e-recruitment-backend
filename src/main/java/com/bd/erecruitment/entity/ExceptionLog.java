package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "EXCEPTION_LOG", indexes = {
		@Index(name = "idx_exception_log_deleted_id", columnList = "deleted, id")
})
@EqualsAndHashCode(callSuper = true)
public class ExceptionLog extends SequenceIdGenerator {

	@Column(name = "trace_id")
	private String traceId;

	@Column(name = "exception_class")
	private String exceptionClass;

	@Column(name = "status_code")
	private int statusCode;

	@Column(name = "request_uri")
	private String requestUri;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	private String message;

	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(name = "stack_trace")
	private String stackTrace;
}
