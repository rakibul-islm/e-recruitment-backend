package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.User;

public interface OtpService {

	// Sets a fresh otpCode/otpExpiry/otpAttempts on the user; caller persists.
	String generate(User user);

	// Validates against the pending OTP (match/expiry/attempts); doesn't clear on success — caller consumes.
	void validate(User user, String otp);
}
