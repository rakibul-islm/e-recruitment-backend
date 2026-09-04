package com.bd.erecruitment.service;

import java.util.Date;

public interface MailService {

	void sendOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendSignupOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendChangePasswordOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendAccountSetupEmail(String toEmail, String fullName, String link, long expiryHours);

	void sendApplicationReceivedEmail(String toEmail, String fullName, String jobTitle);

	void sendApplicationStatusChangedEmail(String toEmail, String fullName, String jobTitle, String status, String note);

	void sendNewApplicationEmail(String toEmail, String jobTitle, String candidateName);

	void sendInterviewScheduledEmail(String toEmail, String fullName, String jobTitle, String interviewTitle, Date scheduledAt, String mode, String location);

	void sendOfferEmail(String toEmail, String fullName, String jobTitle);

	void sendOfferResponseEmail(String toEmail, String jobTitle, String candidateName, boolean accepted);

	void sendJobAlertDigestEmail(String toEmail, String fullName, java.util.List<String> jobTitles);

	void sendRecruiterApplicationReceivedEmail(String toEmail, String fullName, String companyName);

	void sendRecruiterApplicationRejectedEmail(String toEmail, String fullName, String note);
}
