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
@Table(name = "USER_GROUP")
@EqualsAndHashCode(callSuper = true, exclude = {"roles"})
public class UserGroup extends SequenceIdGenerator {

	@Column(nullable = false)
	private String name;

	private String description;

	@Builder.Default
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "USER_GROUP_ROLE",
		joinColumns = @JoinColumn(name = "group_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	private Set<Role> roles = new HashSet<>();
}
