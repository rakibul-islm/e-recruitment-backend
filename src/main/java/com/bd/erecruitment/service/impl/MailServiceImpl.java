package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.service.MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class MailServiceImpl implements MailService {

	private final JavaMailSender mailSender;

	@Value("${app.mail.from}")
	private String fromAddress;

	public MailServiceImpl(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

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
			MimeMessage mimeMessage = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
			helper.setFrom(fromAddress);
			helper.setTo(toEmail);
			helper.setSubject(subject);
			helper.setText(body, true);
			mailSender.send(mimeMessage);
		} catch (MessagingException e) {
			throw new IllegalStateException("Failed to build email from template " + templateFile, e);
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
