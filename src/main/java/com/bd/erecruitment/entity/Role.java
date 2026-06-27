package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "ROLE")
@EqualsAndHashCode(callSuper = true, exclude = {"permissions"})
public class Role extends SequenceIdGenerator {

	@Column(nullable = false)
	private String name;

	@Column(unique = true, nullable = false)
	private String code;

	private String description;

	@Builder.Default
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "ROLE_PERMISSION",
		joinColumns = @JoinColumn(name = "role_id"),
		inverseJoinColumns = @JoinColumn(name = "permission_id")
	)
	private Set<Permission> permissions = new HashSet<>();
}
