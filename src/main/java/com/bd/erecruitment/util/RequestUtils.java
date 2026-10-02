package com.bd.erecruitment.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RequestUtils {

	private static final List<String> BROWSER_NAMES = List.of("Edge", "Opera", "Firefox", "Chrome", "Safari");
	private static final List<Pattern> BROWSER_PATTERNS = Stream.of("Edg/(\\d+)", "OPR/(\\d+)", "Firefox/(\\d+)", "Chrome/(\\d+)", "Version/(\\d+).*Safari")
			.map(Pattern::compile).toList();

	private RequestUtils() {}

	public static String getClientTerminal() {
		HttpServletRequest request = currentRequest();
		return request == null ? null : getClientTerminal(request);
	}

	public static String getClientTerminal(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
		return request.getRemoteAddr();
	}

	public static String getClientLocation() {
		return getClientInfo().location();
	}

	public static String getClientUserAgent() {
		String userAgent = getUserAgent();
		return userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
	}

	public static String getClientDevice() {
		return getClientInfo().device();
	}

	public static ClientInfo getClientInfo() {
		HttpServletRequest request = currentRequest();
		String city = request == null ? null : headerValue(request, "X-Client-City");
		String country = request == null ? null : headerValue(request, "X-Client-Country");
		return parseUserAgent(getUserAgent(), city, country);
	}

	static ClientInfo parseUserAgent(String userAgent, String city, String country) {
		if (userAgent == null || userAgent.isBlank()) return new ClientInfo(city, country, null, null, null);
		String browser = detectBrowser(userAgent);
		String os;
		if (userAgent.contains("Windows")) os = "Windows";
		else if (userAgent.contains("Android")) os = "Android";
		else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) os = "iOS";
		else if (userAgent.contains("Mac OS X")) os = "macOS";
		else if (userAgent.contains("Linux")) os = "Linux";
		else os = "Unknown";
		String type = userAgent.contains("iPad") || userAgent.contains("Tablet") ? "Tablet"
				: userAgent.contains("Mobi") ? "Mobile" : "Desktop";
		return new ClientInfo(city, country, type, os, browser);
	}

	private static String headerValue(HttpServletRequest request, String name) {
		String value = request.getHeader(name);
		if (value == null || value.isBlank()) return null;
		String decoded = URLDecoder.decode(value, StandardCharsets.UTF_8).trim();
		return decoded.length() > 100 ? decoded.substring(0, 100) : decoded;
	}

	private static String detectBrowser(String userAgent) {
		for (int i = 0; i < BROWSER_PATTERNS.size(); i++) {
			Matcher matcher = BROWSER_PATTERNS.get(i).matcher(userAgent);
			if (matcher.find()) return BROWSER_NAMES.get(i) + " " + matcher.group(1);
		}
		return "Unknown";
	}

	public static String getUserAgent() {
		HttpServletRequest request = currentRequest();
		return request == null ? null : request.getHeader("User-Agent");
	}

	public static String getRequestUri() {
		HttpServletRequest request = currentRequest();
		return request == null ? null : request.getRequestURI();
	}

	public static String getHttpMethod() {
		HttpServletRequest request = currentRequest();
		return request == null ? null : request.getMethod();
	}

	private static HttpServletRequest currentRequest() {
		return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
				? attrs.getRequest()
				: null;
	}
}
