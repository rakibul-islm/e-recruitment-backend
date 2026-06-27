package com.bd.erecruitment.exception;

import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;
import lombok.extern.slf4j.Slf4j;
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

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<?> handleApiException(ApiException ex) {
		return respond(ex.getCode(), ex.getMessage());
	}

	@ExceptionHandler(ServiceException.class)
	public ResponseEntity<?> handleServiceException(ServiceException ex) {
		return respond(400, ex.getMessage());
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<?> handleUnreadableMessage(HttpMessageNotReadableException ex) {
		return respond(400, "Malformed or missing request body");
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
	public ResponseEntity<?> handleDataIntegrity(DataIntegrityViolationException ex) {
		return respond(409, "Data conflict: a record with the same unique value already exists");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<?> handleGeneral(Exception ex) {
		log.error("Unhandled exception: {}", ex.getMessage(), ex);
		return respond(500, "An unexpected error occurred");
	}

	private ResponseEntity<Response<Object>> respond(int code, String message) {
		Response<Object> res = new Response<>();
		res.setCode(code);
		res.setSuccess(false);
		res.setMessage(message);
		return ResponseEntity.status(code).body(res);
	}
}
