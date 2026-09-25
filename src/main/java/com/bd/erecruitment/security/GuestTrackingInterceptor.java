package com.bd.erecruitment.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class GuestTrackingInterceptor implements HandlerInterceptor {

	public static final String GUEST_ID_ATTRIBUTE = "guestId";

	private static final String COOKIE_NAME = "GSID";
	private static final int COOKIE_MAX_AGE_SECONDS = 30 * 60;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) return true;

		String guestId = readGuestId(request);
		if (guestId == null) {
			guestId = UUID.randomUUID().toString();
			Cookie cookie = new Cookie(COOKIE_NAME, guestId);
			cookie.setHttpOnly(true);
			cookie.setPath("/");
			cookie.setMaxAge(COOKIE_MAX_AGE_SECONDS);
			response.addCookie(cookie);
		}
		request.setAttribute(GUEST_ID_ATTRIBUTE, guestId);
		return true;
	}

	private String readGuestId(HttpServletRequest request) {
		if (request.getCookies() == null) return null;
		for (Cookie cookie : request.getCookies()) {
			if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
		}
		return null;
	}
}
