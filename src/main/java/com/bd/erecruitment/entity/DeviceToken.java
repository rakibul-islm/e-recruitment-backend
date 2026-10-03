package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@Table(name = "DEVICE_TOKEN", indexes = {
	@Index(name = "idx_device_token_user", columnList = "user_id"),
	@Index(name = "idx_device_token_token", columnList = "push_token", unique = true)
})
public class DeviceToken extends SequenceIdGenerator {

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "push_token", nullable = false, length = 512)
	private String pushToken;

	@Column(name = "platform", length = 20)
	private String platform;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "last_seen_on")
	private Date lastSeenOn;
}
