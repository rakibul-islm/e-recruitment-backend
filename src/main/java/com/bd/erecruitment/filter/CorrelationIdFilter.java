package com.bd.erecruitment.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Assigns every request a correlation id (reusing the caller's X-Correlation-Id header if
 * present), puts it in MDC for the console log pattern, echoes it back as a response header, and
 * leaves it available for AuditLogWriter/ExceptionLogWriter to tag their rows with — so one
 * request's audit row, exception row, and raw logs can all be tied together. Registered before
 * JwtAutenticationFilter so even 401s get one.
 */
@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Correlation-Id";
	public static final String MDC_KEY = "correlationId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String correlationId = request.getHeader(HEADER);
		if (correlationId == null || correlationId.isBlank()) correlationId = UUID.randomUUID().toString();

		MDC.put(MDC_KEY, correlationId);
		response.setHeader(HEADER, correlationId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}
}
