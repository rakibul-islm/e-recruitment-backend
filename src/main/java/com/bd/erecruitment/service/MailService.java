package com.bd.erecruitment.service;

public interface MailService {

	void sendOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendSignupOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendChangePasswordOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendAccountSetupEmail(String toEmail, String fullName, String link, long expiryHours);
}
