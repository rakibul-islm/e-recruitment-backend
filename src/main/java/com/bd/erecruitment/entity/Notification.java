package com.bd.erecruitment.entity;

import com.bd.erecruitment.enums.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "NOTIFICATION", indexes = @Index(name = "idx_notification_recipient", columnList = "recipient_user_id, deleted, read_on"))
public class Notification extends SequenceIdGenerator {

	@Column(name = "recipient_user_id", nullable = false)
	private Long recipientUserId;

	@Enumerated(EnumType.STRING)
	@Column(name = "notification_type", nullable = false, length = 50)
	private NotificationType type;

	@Column(name = "action_route")
	private String actionRoute;

	// 4000 keeps this a plain VARCHAR2 on Oracle, same convention as JobCircular's jobRequirement.
	@Column(name = "params_json", length = 4000)
	private String paramsJson;

	@Column(name = "dedupe_key", length = 150)
	private String dedupeKey;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "read_on")
	private Date readOn;
}
