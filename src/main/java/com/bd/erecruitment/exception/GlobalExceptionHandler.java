package com.bd.erecruitment.exception;

import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.util.DisconnectedClientHelper;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

	private static final Pattern SQL_STATEMENT_SUFFIX = Pattern.compile("(?is);?\\s*sql statement:.*$");

	private final ExceptionLogWriter exceptionLogWriter;

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

	@ExceptionHandler(AsyncRequestNotUsableException.class)
	public ResponseEntity<?> handleClientDisconnect(AsyncRequestNotUsableException ex) {
		return null;
	}

	@ExceptionHandler(AsyncRequestTimeoutException.class)
	public ResponseEntity<?> handleAsyncTimeout(AsyncRequestTimeoutException ex) {
		return null;
	}

	@ExceptionHandler(IOException.class)
	public ResponseEntity<?> handleIo(IOException ex, HttpServletRequest request) {
		if (isClientDisconnect(ex)) return null;
		return handleGeneral(ex, request);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneral(Exception ex, HttpServletRequest request) {
		return respondWithTrace(500, "An unexpected error occurred", ex, request);
	}

	private boolean isClientDisconnect(IOException ex) {
		if (DisconnectedClientHelper.isClientDisconnectedException(ex)) return true;
		// Windows wording, which Spring's helper does not recognise
		String message = ex.getMessage();
		return message != null && message.toLowerCase().contains("connection was aborted");
	}

	private ResponseEntity<Response<Object>> respond(int code, String message) {
		Response<Object> res = new Response<>();
		res.setCode(code);
		res.setSuccess(false);
		res.setMessage(message);
		return ResponseEntity.status(code).body(res);
	}

	private ResponseEntity<Response<Object>> respondWithTrace(int code, String fallbackMessage, Exception ex, HttpServletRequest request) {
		String userMessage = rootMessage(ex, fallbackMessage);
		String traceId = exceptionLogWriter.log(ex, code, userMessage, request.getRequestURI());
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
}
