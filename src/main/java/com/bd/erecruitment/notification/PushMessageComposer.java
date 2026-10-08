package com.bd.erecruitment.notification;

import com.bd.erecruitment.enums.NotificationType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PushMessageComposer {

	public record PushMessage(String title, String body) {}

	private static final String RESOURCE = "/notification-push.properties";
	private static final String DEFAULT_TITLE = "New notification";
	private static final String DEFAULT_BODY = "Open the app to view it.";
	private static final int MAX_BODY_LENGTH = 300;
	private static final Pattern BLOCK_BREAK = Pattern.compile("(?i)</(p|div|li|h[1-6])>|<br\\s*/?>");
	private static final Pattern TAG = Pattern.compile("<[^>]+>");
	private static final Pattern PLACEHOLDER = Pattern.compile("[{][{]([A-Za-z0-9_]+)[}][}]");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH).withZone(ZoneOffset.UTC);

	private final Properties templates = load();

	public PushMessage compose(NotificationType type, Map<String, Object> params) {
		String title = fill(templates.getProperty(type + ".title", DEFAULT_TITLE), params);
		String body = plainText(fill(templates.getProperty(type + ".message", DEFAULT_BODY), params));
		return new PushMessage(title, StringUtils.abbreviate(body, MAX_BODY_LENGTH));
	}

	private String plainText(String html) {
		String text = TAG.matcher(BLOCK_BREAK.matcher(html).replaceAll(" ")).replaceAll("");
		text = text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
		return text.replaceAll("\\s+", " ").trim();
	}

	private String fill(String template, Map<String, Object> params) {
		Matcher matcher = PLACEHOLDER.matcher(template);
		StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(result, Matcher.quoteReplacement(display(matcher.group(1), params.get(matcher.group(1)))));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private String display(String key, Object value) {
		if (value == null) return "";
		String text = String.valueOf(value);
		if (key.endsWith("At")) return format(text, DATE_TIME);
		if (key.endsWith("Date")) return format(text, DATE);
		return text;
	}

	private String format(String text, DateTimeFormatter formatter) {
		try {
			return formatter.format(Instant.parse(text));
		} catch (DateTimeParseException e) {
			return text;
		}
	}

	private Properties load() {
		Properties properties = new Properties();
		try (InputStream in = PushMessageComposer.class.getResourceAsStream(RESOURCE)) {
			if (in != null) properties.load(in);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load " + RESOURCE, e);
		}
		return properties;
	}
}
