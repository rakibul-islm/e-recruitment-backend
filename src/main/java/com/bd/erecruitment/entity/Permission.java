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
@Table(name = "PERMISSION")
@EqualsAndHashCode(callSuper = true)
public class Permission extends SequenceIdGenerator {

	@Column(nullable = false)
	private String name;

	@Column(unique = true, nullable = false)
	private String authority; // e.g. "user:read", "job:write" — used by Spring Security + Angular AccessGuard

	private String routeName; // Angular route name — AccessGuard matches this to current route

	private String description;

	private String module; // grouping e.g. "USER_MANAGEMENT"
}
