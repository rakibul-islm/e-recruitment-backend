package com.bd.erecruitment.seed.data;

public class PasswordPolicyData {

	public record PolicyDef(
		int minLength, int maxLength,
		boolean requireUppercase, boolean requireLowercase,
		boolean requireDigit, boolean requireSpecialChar,
		boolean disallowUserInfoInPassword
	) {}

	public static PolicyDef getDefault() {
		return new PolicyDef(
			1, 1,
			false, false, false, false,
			true
		);
	}
}
