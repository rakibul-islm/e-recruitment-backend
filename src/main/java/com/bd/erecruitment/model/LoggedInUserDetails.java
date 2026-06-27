package com.bd.erecruitment.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class LoggedInUserDetails {

	private Long id;
	private String username;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean enabled;
	private boolean locked;
	private Date expiryDate;
	private List<String> authorities; // authority codes for Angular AccessGuard
}
