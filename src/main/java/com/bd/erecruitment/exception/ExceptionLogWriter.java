package com.bd.erecruitment.exception;

import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.filter.CorrelationIdFilter;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.service.impl.SystemConfigServiceImpl;
import com.bd.erecruitment.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExceptionLogWriter {

	private static final String EXCEPTION_LOG_CONFIG_KEY = "EXCEPTION_LOG_TO_DB";
	// stack_trace is a bounded varchar(32600); truncate so the insert itself cannot fail.
	private static final int STACK_TRACE_MAX_LENGTH = 32000;

	private final SystemConfigServiceImpl systemConfigService;
	private final ExceptionLogRepo exceptionLogRepo;

	public String log(Exception ex, int statusCode, String message, String context) {
		String traceId = UUID.randomUUID().toString();
		log.error("[{}] {}: {} ({})", traceId, ex.getClass().getSimpleName(), message, context, ex);
		try {
			SystemConfig config = systemConfigService.findCachedByKey(EXCEPTION_LOG_CONFIG_KEY);
			boolean enabled = config != null && "Y".equalsIgnoreCase(config.getConfigValue());
			if (!enabled) return traceId;

			StringWriter sw = new StringWriter();
			ex.printStackTrace(new PrintWriter(sw));
			String stackTrace = sw.toString();
			if (stackTrace.length() > STACK_TRACE_MAX_LENGTH) {
				stackTrace = stackTrace.substring(0, STACK_TRACE_MAX_LENGTH) + "\n... truncated";
			}

			Date now = new Date();
			String terminal = RequestUtils.getClientTerminal();
			ExceptionLog entry = new ExceptionLog();
			entry.setTraceId(traceId)
					.setExceptionClass(ex.getClass().getName())
					.setStatusCode(statusCode)
					.setRequestUri(context)
					.setCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY))
					.setMessage(message)
					.setStackTrace(stackTrace)
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
