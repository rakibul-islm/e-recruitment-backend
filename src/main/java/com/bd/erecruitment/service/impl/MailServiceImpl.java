package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.JobAlertItemDto;
import com.bd.erecruitment.service.MailService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MailServiceImpl implements MailService {

	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

	private final RestClient restClient = RestClient.create();
	private final ReentrantLock tokenLock = new ReentrantLock();
	private final Map<String, String> templateCache = new ConcurrentHashMap<>();

	private volatile String cachedAccessToken;
	private volatile Instant cachedTokenExpiry = Instant.EPOCH;

	@Value("${app.mail.from}")
	private String fromAddress;

	@Value("${app.mail.gmail.client-id}")
	private String clientId;

	@Value("${app.mail.gmail.client-secret}")
	private String clientSecret;

	@Value("${app.mail.gmail.refresh-token}")
	private String refreshToken;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	@Override
	public void sendOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes) {
		sendTemplateEmail(toEmail, "forgot-password-otp-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"otp", otp,
			"expiryMinutes", String.valueOf(expiryMinutes)
		));
	}

	@Override
	public void sendSignupOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes) {
		sendTemplateEmail(toEmail, "signup-otp-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"otp", otp,
			"expiryMinutes", String.valueOf(expiryMinutes)
		));
	}

	@Override
	public void sendChangePasswordOtpEmail(String toEmail, String fullName, String otp, long expiryMinutes) {
		sendTemplateEmail(toEmail, "change-password-otp-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"otp", otp,
			"expiryMinutes", String.valueOf(expiryMinutes)
		));
	}

	@Override
	public void sendAccountSetupEmail(String toEmail, String fullName, String link, long expiryHours) {
		sendTemplateEmail(toEmail, "account-setup-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"link", link,
			"expiryHours", String.valueOf(expiryHours)
		));
	}

	@Override
	public void sendApplicationReceivedEmail(String toEmail, String fullName, String jobTitle, String applicationLink) {
		sendTemplateEmail(toEmail, "application-received-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"jobTitle", jobTitle,
			"applicationLink", applicationLink
		));
	}

	@Override
	public void sendApplicationStatusChangedEmail(String toEmail, String fullName, String jobTitle, String status, String note, String applicationLink) {
		sendTemplateEmail(toEmail, "application-status-changed-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"jobTitle", jobTitle,
			"status", status,
			"note", StringUtils.defaultIfBlank(note, ""),
			"applicationLink", applicationLink
		));
	}

	@Override
	public void sendNewApplicationEmail(String toEmail, String jobTitle, String candidateName, String applicationLink) {
		sendTemplateEmail(toEmail, "new-application-email.html", Map.of(
			"jobTitle", jobTitle,
			"candidateName", candidateName,
			"applicationLink", applicationLink
		));
	}

	@Override
	public void sendInterviewScheduledEmail(String toEmail, String fullName, String jobTitle, String interviewTitle,
			java.util.Date scheduledAt, String mode, String location, String applicationLink) {
		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm");
		sendTemplateEmail(toEmail, "interview-scheduled-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"jobTitle", jobTitle,
			"interviewTitle", interviewTitle,
			"scheduledAt", scheduledAt != null ? format.format(scheduledAt) : "TBD",
			"mode", StringUtils.defaultIfBlank(mode, "TBD"),
			"location", StringUtils.defaultIfBlank(location, ""),
			"applicationLink", applicationLink
		));
	}

	@Override
	public void sendOfferEmail(String toEmail, String fullName, String jobTitle, String applicationLink) {
		sendTemplateEmail(toEmail, "offer-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"jobTitle", jobTitle,
			"applicationLink", applicationLink
		));
	}

	@Override
	public void sendOfferResponseEmail(String toEmail, String jobTitle, String candidateName, boolean accepted, String applicationLink) {
		sendTemplateEmail(toEmail, "offer-response-email.html", Map.of(
			"jobTitle", jobTitle,
			"candidateName", candidateName,
			"decision", accepted ? "ACCEPTED" : "DECLINED",
			"applicationLink", applicationLink
		));
	}

	@Override
	public void sendJobAlertDigestEmail(String toEmail, String fullName, List<JobAlertItemDto> jobs) {
		sendTemplateEmail(toEmail, "job-alert-digest-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"jobItems", buildJobItemsHtml(jobs),
			"jobCount", String.valueOf(jobs.size()),
			"browseAllLink", frontendBaseUrl + "/jobs"
		), Set.of("jobItems"));
	}

	private String buildJobItemsHtml(List<JobAlertItemDto> jobs) {
		StringBuilder html = new StringBuilder();
		for (JobAlertItemDto job : jobs) {
			html.append("<tr><td style=\"padding:0 0 12px;\">")
				.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background-color:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;\">")
				.append("<tr><td style=\"padding:16px 20px;\">")
				.append("<a href=\"").append(escape(job.jobLink())).append("\" style=\"display:block;margin:0 0 4px;color:#1d4ed8;font-size:15px;font-weight:600;text-decoration:none;\">")
				.append(escape(job.jobTitle())).append("</a>");

			String companyLine = StringUtils.join(java.util.stream.Stream.of(job.companyName(), job.jobLocation())
				.filter(StringUtils::isNotBlank).toList(), " · ");
			if (StringUtils.isNotBlank(companyLine)) {
				html.append("<p style=\"margin:0 0 10px;color:#6b7280;font-size:13px;\">").append(escape(companyLine)).append("</p>");
			}

			if (StringUtils.isNotBlank(job.employmentType()) || StringUtils.isNotBlank(job.applicationDeadline())) {
				html.append("<p style=\"margin:0;font-size:12px;\">");
				if (StringUtils.isNotBlank(job.employmentType())) {
					html.append("<span style=\"display:inline-block;padding:2px 10px;background-color:#e0e7ff;color:#1d4ed8;border-radius:12px;font-weight:600;margin-right:8px;\">")
						.append(escape(job.employmentType())).append("</span>");
				}
				if (StringUtils.isNotBlank(job.applicationDeadline())) {
					html.append("<span style=\"color:#9ca3af;\">Apply by ").append(escape(job.applicationDeadline())).append("</span>");
				}
				html.append("</p>");
			}

			html.append("</td></tr></table></td></tr>");
		}
		return html.toString();
	}

	@Override
	public void sendRecruiterApplicationReceivedEmail(String toEmail, String fullName, String companyName) {
		sendTemplateEmail(toEmail, "recruiter-application-received-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"companyName", companyName
		));
	}

	@Override
	public void sendRecruiterApplicationRejectedEmail(String toEmail, String fullName, String note) {
		sendTemplateEmail(toEmail, "recruiter-application-rejected-email.html", Map.of(
			"greetingName", greetingName(fullName),
			"note", StringUtils.defaultIfBlank(note, "")
		));
	}

	private String greetingName(String fullName) {
		return StringUtils.isNotBlank(fullName) ? fullName : "there";
	}

	private void sendTemplateEmail(String toEmail, String templateFile, Map<String, String> values) {
		sendTemplateEmail(toEmail, templateFile, values, Set.of());
	}

	private void sendTemplateEmail(String toEmail, String templateFile, Map<String, String> values, Set<String> rawKeys) {
		String template = loadTemplate(templateFile);

		int firstNewline = template.indexOf('\n');
		String subjectLine = template.substring(0, firstNewline).trim();
		if (!subjectLine.startsWith("Subject:")) {
			throw new IllegalStateException("Email template " + templateFile + " is missing a leading 'Subject:' line");
		}
		String subject = substitute(subjectLine.substring("Subject:".length()).trim(), values, rawKeys);
		String body = substitute(template.substring(firstNewline + 1), values, rawKeys);

		try {
			restClient.post()
				.uri(SEND_URL)
				.header("Authorization", "Bearer " + getAccessToken())
				.body(Map.of("raw", buildRawMessage(toEmail, subject, body)))
				.retrieve()
				.onStatus(HttpStatusCode::isError, (req, res) -> {
					String responseBody;
					try {
						responseBody = new String(res.getBody().readAllBytes(), StandardCharsets.UTF_8);
					} catch (IOException e) {
						responseBody = "<unreadable response body>";
					}
					throw new IllegalStateException("Gmail API returned " + res.getStatusCode() + ": " + responseBody);
				})
				.toBodilessEntity();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to send email via Gmail API for template " + templateFile, e);
		}
	}

	private String buildRawMessage(String toEmail, String subject, String htmlBody) {
		String raw = "From: " + fromAddress + "\r\n" +
			"To: " + toEmail + "\r\n" +
			"Subject: " + subject + "\r\n" +
			"MIME-Version: 1.0\r\n" +
			"Content-Type: text/html; charset=\"UTF-8\"\r\n" +
			"\r\n" +
			htmlBody;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	@SuppressWarnings("unchecked")
	private String getAccessToken() {
		if (cachedAccessToken != null && Instant.now().isBefore(cachedTokenExpiry)) {
			return cachedAccessToken;
		}
		tokenLock.lock();
		try {
			if (cachedAccessToken != null && Instant.now().isBefore(cachedTokenExpiry)) {
				return cachedAccessToken;
			}
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("client_id", clientId);
			form.add("client_secret", clientSecret);
			form.add("refresh_token", refreshToken);
			form.add("grant_type", "refresh_token");

			Map<String, Object> response = restClient.post()
				.uri(TOKEN_URL)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(Map.class);

			cachedAccessToken = (String) response.get("access_token");
			int expiresInSeconds = (Integer) response.get("expires_in");
			cachedTokenExpiry = Instant.now().plusSeconds(expiresInSeconds - 60L);
			return cachedAccessToken;
		} finally {
			tokenLock.unlock();
		}
	}

	private String substitute(String text, Map<String, String> values, Set<String> rawKeys) {
		for (Map.Entry<String, String> entry : values.entrySet()) {
			String replacement = rawKeys.contains(entry.getKey()) ? entry.getValue() : escape(entry.getValue());
			text = text.replace("{{" + entry.getKey() + "}}", replacement);
		}
		return text;
	}

	private String loadTemplate(String templateFile) {
		return templateCache.computeIfAbsent(templateFile, this::readTemplate);
	}

	private String readTemplate(String templateFile) {
		String classpathLocation = "templates/email/" + templateFile;
		try (InputStream in = new ClassPathResource(classpathLocation).getInputStream()) {
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to load email template: " + classpathLocation, e);
		}
	}

	private String escape(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
