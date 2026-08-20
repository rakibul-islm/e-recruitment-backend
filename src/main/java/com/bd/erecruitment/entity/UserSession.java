package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "USER_SESSION", indexes = {
		@Index(name = "idx_user_session_jti", columnList = "jti", unique = true),
		@Index(name = "idx_user_session_user_revoked", columnList = "user_id, revoked")
})
@EqualsAndHashCode(callSuper = true, exclude = "user")
public class UserSession extends SequenceIdGenerator {

	@Column(name = "jti", unique = true, nullable = false, length = 64)
	private String jti;

	@ToString.Exclude
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Temporal(TemporalType.TIMESTAMP)
	private Date issuedAt;

	@Temporal(TemporalType.TIMESTAMP)
	private Date expiresAt;

	@Column(length = 100)
	private String ipAddress;

	@Column(length = 255)
	private String userAgent;

	private boolean revoked;

	@Temporal(TemporalType.TIMESTAMP)
	private Date revokedAt;

	@Column(length = 100)
	private String revokedBy;
}
