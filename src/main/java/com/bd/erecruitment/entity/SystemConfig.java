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
@Table(name = "SYSTEM_CONFIG")
@EqualsAndHashCode(callSuper = true)
public class SystemConfig extends SequenceIdGenerator {

	@Column(name = "config_key", unique = true, nullable = false)
	private String configKey;

	@Column(name = "config_value", nullable = false)
	private String configValue;

	private String description;

	@Column(name = "expected_values")
	private String expectedValues; // comma-separated allowed values, e.g. "Y,N" — drives a dropdown instead of free text on the frontend
}
