package com.bd.erecruitment.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class RequestUtils {

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
