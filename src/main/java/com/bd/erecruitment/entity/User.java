package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "USER_ACCOUNT")
@EqualsAndHashCode(callSuper = true, exclude = {"roles", "userGroups"})
public class User extends SequenceIdGenerator {

	private String fullName;

	@Column(name = "username", unique = true)
	private String username;

	private String password;

	@Column(name = "email", unique = true)
	private String email;

	private String address;
	private String phone;
	private String mobile;
	private boolean active;
	private boolean locked;

	@Temporal(TemporalType.DATE)
	private Date expiryDate;

	@Lob
	@Column(name = "filedata")
	private byte[] fileData;

	@Transient
	private String imageBase64;

	@Column(name = "google_id", unique = true)
	private String googleId;

	@Builder.Default
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "USER_ROLE",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	private Set<Role> roles = new HashSet<>();

	@Builder.Default
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "USER_USER_GROUP",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "group_id")
	)
	private Set<UserGroup> userGroups = new HashSet<>();
}
