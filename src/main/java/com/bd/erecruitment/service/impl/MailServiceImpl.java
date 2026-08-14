package com.bd.erecruitment.service.impl;

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
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MailServiceImpl implements MailService {

	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String SEND_URL = "https://gmail.googleapis.com/gmail/v1/users/me/messages/send";

	private final RestClient restClient = RestClient.create();
	private final ReentrantLock tokenLock = new ReentrantLock();

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

	private String greetingName(String fullName) {
		return StringUtils.isNotBlank(fullName) ? fullName : "there";
	}

	private void sendTemplateEmail(String toEmail, String templateFile, Map<String, String> values) {
		String template = loadTemplate(templateFile);

		int firstNewline = template.indexOf('\n');
		String subjectLine = template.substring(0, firstNewline).trim();
		if (!subjectLine.startsWith("Subject:")) {
			throw new IllegalStateException("Email template " + templateFile + " is missing a leading 'Subject:' line");
		}
		String subject = substitute(subjectLine.substring("Subject:".length()).trim(), values);
		String body = substitute(template.substring(firstNewline + 1), values);

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

	private String substitute(String text, Map<String, String> values) {
		for (Map.Entry<String, String> entry : values.entrySet()) {
			text = text.replace("{{" + entry.getKey() + "}}", escape(entry.getValue()));
		}
		return text;
	}

	private String loadTemplate(String templateFile) {
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
