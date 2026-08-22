package com.bd.erecruitment.audit;

public final class AuditAction {

	private AuditAction() {}

	public static final String CREATE = "CREATE";
	public static final String UPDATE = "UPDATE";
	public static final String SOFT_DELETE = "SOFT_DELETE";
	public static final String HARD_DELETE = "HARD_DELETE";

	public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
	public static final String LOGIN_FAILURE = "LOGIN_FAILURE";
	public static final String LOGOUT = "LOGOUT";
	public static final String FORCE_LOGOUT = "FORCE_LOGOUT";
	public static final String PASSWORD_RESET = "PASSWORD_RESET";
	public static final String PASSWORD_SET = "PASSWORD_SET";
	public static final String OTP_VERIFIED = "OTP_VERIFIED";
	public static final String OTP_FAILED = "OTP_FAILED";
}
