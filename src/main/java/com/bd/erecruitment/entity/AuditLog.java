package com.bd.erecruitment.entity;

import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "AUDIT_LOG", indexes = {
		@Index(name = "idx_audit_log_entity", columnList = "entity_type, entity_id"),
		@Index(name = "idx_audit_log_actor", columnList = "created_by, created_on"),
		@Index(name = "idx_audit_log_created_on", columnList = "created_on"),
		@Index(name = "idx_audit_log_category_action", columnList = "category, action"),
		@Index(name = "idx_audit_log_correlation", columnList = "correlation_id")
})
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends SequenceIdGenerator {

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 20)
	private AuditCategory category;

	@Column(name = "action", nullable = false, length = 30)
	private String action;

	@Column(name = "entity_type", length = 100)
	private String entityType;

	@Column(name = "entity_id")
	private Long entityId;

	@Enumerated(EnumType.STRING)
	@Column(name = "outcome", nullable = false, length = 10)
	private AuditOutcome outcome;

	@Column(name = "ip_address", length = 100)
	private String ipAddress;

	@Column(name = "user_agent", length = 500)
	private String userAgent;

	@Column(name = "correlation_id", length = 100)
	private String correlationId;

	@Column(name = "request_uri", length = 500)
	private String requestUri;

	@Column(name = "http_method", length = 10)
	private String httpMethod;

	// Reserved for future per-entity diffing; not populated in phase 1 (see plan's Performance impact notes).
	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(name = "changed_fields")
	private String changedFields;
}
