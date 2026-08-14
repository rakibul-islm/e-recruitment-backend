package com.bd.erecruitment.exception;

import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.repository.SystemConfigRepo;
import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.RequestUtils;
import com.bd.erecruitment.util.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private static final String EXCEPTION_LOG_CONFIG_KEY = "EXCEPTION_LOG_TO_DB";
	private static final Pattern SQL_STATEMENT_SUFFIX = Pattern.compile("(?is);?\\s*sql statement:.*$");

	private final SystemConfigRepo systemConfigRepo;
	private final ExceptionLogRepo exceptionLogRepo;

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<?> handleApiException(ApiException ex) {
		return respond(ex.getCode(), ex.getMessage());
	}

	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<?> handleServiceException(ServiceException ex) {
		return respond(400, ex.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleUnreadableMessage(HttpMessageNotReadableException ex, HttpServletRequest request) {
		return respondWithTrace(400, "Malformed or missing request body", ex, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.collect(Collectors.joining(", "));
		return respond(400, message);
	}

	@ExceptionHandler({UsernameNotFoundException.class, BadCredentialsException.class, InsufficientAuthenticationException.class})
	public ResponseEntity<?> handleUnauthorized(Exception ex) {
		return respond(401, ex.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
		return respond(403, "Access denied");
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<?> handleNotFound(NoSuchElementException ex) {
		return respond(404, ex.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
		return respondWithTrace(409, "A record with a conflicting value already exists", ex, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest request) {
		return respondWithTrace(500, "An unexpected error occurred", ex, request);
	}

	private ResponseEntity<Response<Object>> respond(int code, String message) {
		Response<Object> res = new Response<>();
		res.setCode(code);
		res.setSuccess(false);
		res.setMessage(message);
		return ResponseEntity.status(code).body(res);
	}

	private ResponseEntity<Response<Object>> respondWithTrace(int code, String fallbackMessage, Exception ex, HttpServletRequest request) {
		String traceId = UUID.randomUUID().toString();
		String userMessage = rootMessage(ex, fallbackMessage);
		log.error("[{}] {}: {}", traceId, ex.getClass().getSimpleName(), ex.getMessage(), ex);
		persistIfEnabled(traceId, code, userMessage, ex, request);
		Response<Object> res = new Response<>();
		res.setCode(code);
		res.setSuccess(false);
		res.setMessage(userMessage);
		res.setTraceId(traceId);
		return ResponseEntity.status(code).body(res);
	}

	private String rootMessage(Exception ex, String fallback) {
		String message = ex instanceof NestedRuntimeException nre
				? nre.getMostSpecificCause().getMessage()
				: ex.getMessage();
		message = stripSqlStatement(message);
		return StringUtils.isNotBlank(message) ? message : fallback;
	}

	private String stripSqlStatement(String message) {
		if (message == null) return null;
		return SQL_STATEMENT_SUFFIX.matcher(message).replaceAll("").trim();
	}

	private void persistIfEnabled(String traceId, int statusCode, String userMessage, Exception ex, HttpServletRequest request) {
		try {
			boolean enabled = systemConfigRepo.findByConfigKeyAndDeleted(EXCEPTION_LOG_CONFIG_KEY, false)
					.map(c -> "Y".equalsIgnoreCase(c.getConfigValue()))
					.orElse(false);
			if (!enabled) return;

			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));

			Date now = new Date();
			String terminal = RequestUtils.getClientTerminal(request);
			ExceptionLog entry = new ExceptionLog();
			entry.setTraceId(traceId)
					.setExceptionClass(ex.getClass().getName())
					.setStatusCode(statusCode)
					.setRequestUri(request.getRequestURI())
					.setMessage(userMessage)
					.setStackTrace(sw.toString())
					.setCreatedBy("system").setCreatedOn(now).setCreatedTerminal(terminal)
					.setUpdatedBy("system").setUpdatedOn(now).setUpdatedTerminal(terminal)
					.setDeleted(false);
			exceptionLogRepo.save(entry);
		} catch (Exception persistEx) {
			log.error("Failed to persist exception log for traceId {}: {}", traceId, persistEx.getMessage(), persistEx);
		}
	}
}
