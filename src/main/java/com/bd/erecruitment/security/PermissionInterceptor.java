package com.bd.erecruitment.security;

import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.repository.PermissionRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

	private static final Map<String, String> METHOD_ACTION = Map.of(
		"GET",    "read",
		"POST",   "write",
		"PUT",    "write",
		"DELETE", "delete",
		"PATCH",  "write"
	);

	private static final String SUPER_ADMIN = "SUPER_ADMIN";

	private final PermissionRepo permissionRepo;

	// Cache of known authorities to avoid a DB hit on every request.
	// Cleared on write/delete so new permissions are picked up immediately.
	private final Set<String> authorityCache = ConcurrentHashMap.newKeySet();
	private volatile boolean cacheFilled = false;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (!(handler instanceof HandlerMethod)) return true;

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) return true;

		if (hasSuperAdmin(auth)) return true;

		String resource = extractResource(request.getServletPath());
		if (resource.isEmpty()) return true;

		String action = METHOD_ACTION.getOrDefault(request.getMethod().toUpperCase(), "read");
		String required = resource + ":" + action;

		// If the authority is not registered in the Permission table, bypass enforcement.
		if (!isRegistered(required)) return true;

		if (auth.getAuthorities().stream().noneMatch(a -> required.equals(a.getAuthority())))
			throw new ForbiddenException("Access denied: '" + required + "' authority required");

		return true;
	}

	public void invalidateCache() {
		cacheFilled = false;
		authorityCache.clear();
	}

	private boolean isRegistered(String authority) {
		if (!cacheFilled) {
			permissionRepo.findAll().forEach(p -> authorityCache.add(p.getAuthority()));
			cacheFilled = true;
		}
		return authorityCache.contains(authority);
	}

	private boolean hasSuperAdmin(Authentication auth) {
		return auth.getAuthorities().stream().anyMatch(a -> SUPER_ADMIN.equals(a.getAuthority()));
	}

	private String extractResource(String servletPath) {
		String[] parts = servletPath.replaceFirst("^/", "").split("/");
		return parts.length > 0 ? parts[0] : "";
	}
}
