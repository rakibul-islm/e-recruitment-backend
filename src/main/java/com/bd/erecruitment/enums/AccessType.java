package com.bd.erecruitment.enums;

public enum AccessType {

	READ("Read"), WRITE("Write");

	private String des;

	private AccessType(String des) {
		this.des = des;
	}

	public String getDes() {
		return this.des;
	}
}
