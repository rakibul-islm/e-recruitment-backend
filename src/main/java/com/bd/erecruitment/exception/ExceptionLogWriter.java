package com.bd.erecruitment.exception;

import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.repository.SystemConfigRepo;
import com.bd.erecruitment.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;

/**
 * Central point for persisting caught exceptions to the EXCEPTION_LOG table, gated by the
 * EXCEPTION_LOG_TO_DB system config (Y/N). Used both by {@link GlobalExceptionHandler} and by
 * try/catch blocks elsewhere in the codebase that handle an exception without letting it
 * propagate to the controller boundary.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionLogWriter {

	private static final String EXCEPTION_LOG_CONFIG_KEY = "EXCEPTION_LOG_TO_DB";

	private final SystemConfigRepo systemConfigRepo;
	private final ExceptionLogRepo exceptionLogRepo;

	/**
	 * @param statusCode the HTTP status ultimately returned to the caller, or 0 when the
	 *                    exception was fully handled/swallowed and never surfaced as a response
	 * @param context     request URI, or another short description of where the exception was caught
	 * @return a generated trace id, useful for correlating with logs regardless of whether persistence is enabled
	 */
	public String log(Exception ex, int statusCode, String message, String context) {
		String traceId = UUID.randomUUID().toString();
		log.error("[{}] {}: {} ({})", traceId, ex.getClass().getSimpleName(), message, context, ex);
		try {
			boolean enabled = systemConfigRepo.findByConfigKeyAndDeleted(EXCEPTION_LOG_CONFIG_KEY, false)
					.map(c -> "Y".equalsIgnoreCase(c.getConfigValue()))
					.orElse(false);
			if (!enabled) return traceId;

			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));

			Date now = new Date();
			String terminal = RequestUtils.getClientTerminal();
			ExceptionLog entry = new ExceptionLog();
			entry.setTraceId(traceId)
					.setExceptionClass(ex.getClass().getName())
					.setStatusCode(statusCode)
					.setRequestUri(context)
					.setMessage(message)
					.setStackTrace(sw.toString())
					.setCreatedBy("system").setCreatedOn(now).setCreatedTerminal(terminal)
					.setUpdatedBy("system").setUpdatedOn(now).setUpdatedTerminal(terminal)
					.setDeleted(false);
			exceptionLogRepo.save(entry);
		} catch (Exception persistEx) {
			log.error("Failed to persist exception log for traceId {}: {}", traceId, persistEx.getMessage(), persistEx);
		}
		return traceId;
	}

	public String log(Exception ex, String context) {
		return log(ex, 0, ex.getMessage(), context);
	}
}
