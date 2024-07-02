package com.bd.erecruitment.enums;

public enum UserRole {

	ROLE_SUPER_ADMIN("Super Admin"),
	ROLE_SYSTEM_ADMIN("System Admin"),
	ROLE_SND_USER("SND User"),
	ROLE_NORMAL_USER("Normal User");

	private String des;

	private UserRole(String des) {
		this.des = des;
	}

	public String getDes() {
		return this.des;
	}
}
