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
@Table(name = "PASSWORD_POLICY")
@EqualsAndHashCode(callSuper = true)
public class PasswordPolicy extends SequenceIdGenerator {

	@Column(name = "min_length", nullable = false)
	private int minLength;

	@Column(name = "max_length", nullable = false)
	private int maxLength;

	@Column(name = "require_uppercase", nullable = false)
	private boolean requireUppercase;

	@Column(name = "require_lowercase", nullable = false)
	private boolean requireLowercase;

	@Column(name = "require_digit", nullable = false)
	private boolean requireDigit;

	@Column(name = "require_special_char", nullable = false)
	private boolean requireSpecialChar;

	@Column(name = "disallow_user_info_in_password", nullable = false)
	private boolean disallowUserInfoInPassword;
}
