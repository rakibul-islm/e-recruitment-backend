package com.bd.erecruitment.entity;

import com.bd.erecruitment.audit.AuditIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "USER_ACCOUNT")
@EqualsAndHashCode(callSuper = true, exclude = {"roles"})
public class User extends SequenceIdGenerator {

	private String fullName;

	@AuditIgnore
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

	@JdbcTypeCode(SqlTypes.VARBINARY)
	@Column(name = "filedata", length = 5_000_000)
	private byte[] fileData;

	@Transient
	private String imageBase64;

	@Column(name = "google_id", unique = true)
	private String googleId;

	@AuditIgnore
	private String otpCode;

	@AuditIgnore
	private Date otpExpiry;

	@AuditIgnore
	private int otpAttempts;

	@AuditIgnore
	private String activationToken;

	@AuditIgnore
	private Date activationTokenExpiry;

	@Builder.Default
	@ToString.Exclude
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "USER_ROLE",
		joinColumns = @JoinColumn(name = "user_id"),
		inverseJoinColumns = @JoinColumn(name = "role_id")
	)
	private Set<Role> roles = new HashSet<>();

	// No JPA relation to UserGroup on purpose: only seeds the UI's role picklist default, no ongoing meaning.
	@Column(name = "group_id")
	private Long userGroupId;

	// No JPA relation to Company, same convention as userGroupId - a recruiter's employer, set
	// automatically when their RecruiterApplication is approved (or by an admin from the User form).
	// CompanyServiceImpl scopes recruiters' company access to this id.
	@Column(name = "company_id")
	private Long companyId;
}
