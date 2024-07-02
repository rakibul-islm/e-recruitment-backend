package com.bd.erecruitment.entity;

import com.bd.erecruitment.enums.UserRole;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@Entity
@Table(name = "USER_ACCOUNT")
@EqualsAndHashCode(callSuper = true)
public class User extends SequenceIdGenerator{

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

	private boolean superAdmin;
	private boolean systemAdmin;
	private boolean recruiterUser;
	private boolean candidateUser;

	@Transient
	private String roles;

	public String getRoles() {
		this.roles = "";
		if(Boolean.TRUE.equals(superAdmin)) roles += UserRole.ROLE_SUPER_ADMIN.name() + ',';
		if(Boolean.TRUE.equals(systemAdmin)) roles += UserRole.ROLE_SYSTEM_ADMIN.name() + ',';
		if(Boolean.TRUE.equals(recruiterUser)) roles += UserRole.ROLE_RECRUITER_USER.name() + ',';
		if(Boolean.TRUE.equals(candidateUser)) roles += UserRole.ROLE_CANDIDATE_USER.name() + ',';

		if(StringUtils.isBlank(roles)) return roles;

		int lastComma = roles.lastIndexOf(',');
		String finalString = roles.substring(0, lastComma);
		roles = finalString;
		return roles;
	}
}
