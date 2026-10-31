package com.bd.erecruitment.filter;

import com.bd.erecruitment.backup.RestoreState;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RestoreMaintenanceFilter extends OncePerRequestFilter {

	private static final Set<String> ALLOWED_PATHS = Set.of("/backup-restore/status", "/actuator/health");
	private static final String BODY = "{\"code\":503,\"success\":false,\"message\":\"The system is being restored from a backup. Please try again in a few minutes.\"}";

	private final RestoreState restoreState;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (!restoreState.isRunning() || "OPTIONS".equalsIgnoreCase(request.getMethod()) || ALLOWED_PATHS.contains(request.getServletPath())) {
			filterChain.doFilter(request, response);
			return;
		}
		response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
		response.setHeader("Retry-After", "60");
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.getWriter().write(BODY);
	}
}
