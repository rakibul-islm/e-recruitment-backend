package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.JobAlertItemDto;

import java.util.Date;
import java.util.List;

public interface MailService {

	void sendOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendSignupOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendChangePasswordOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes);

	void sendAccountSetupEmail(String toEmail, String fullName, String link, long expiryHours);

	void sendApplicationReceivedEmail(String toEmail, String fullName, String jobTitle, String applicationLink);

	void sendApplicationStatusChangedEmail(String toEmail, String fullName, String jobTitle, String status, String note, String applicationLink);

	void sendNewApplicationEmail(String toEmail, String jobTitle, String candidateName, String applicationLink);

	void sendInterviewScheduledEmail(String toEmail, String fullName, String jobTitle, String interviewTitle, Date scheduledAt, String mode, String location, String applicationLink);

	void sendOfferEmail(String toEmail, String fullName, String jobTitle, String applicationLink);

	void sendOfferResponseEmail(String toEmail, String jobTitle, String candidateName, boolean accepted, String applicationLink);

	void sendJobAlertDigestEmail(String toEmail, String fullName, List<JobAlertItemDto> jobs);

	void sendRecruiterApplicationReceivedEmail(String toEmail, String fullName, String companyName);

	void sendRecruiterApplicationRejectedEmail(String toEmail, String fullName, String note);
}
