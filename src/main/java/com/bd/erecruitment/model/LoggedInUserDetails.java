package com.bd.erecruitment.model;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;

import java.util.Date;
import java.util.List;

@Data
public class LoggedInUserDetails {

	private String username;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean superadmin;
	private boolean systemadmin;
	private boolean snduser;
	private boolean normaluser;
	private String roles;
	private List<GrantedAuthority> authorities;
	private boolean enabled;
	private boolean locked;
	private Date expiryDate;
	private Long admin_id;
}
