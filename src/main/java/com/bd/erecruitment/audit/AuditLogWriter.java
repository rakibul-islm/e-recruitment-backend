package com.bd.erecruitment.audit;

import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.filter.CorrelationIdFilter;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.service.impl.SystemConfigServiceImpl;
import com.bd.erecruitment.util.RequestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Date;
import java.util.concurrent.Executor;

/**
 * Central point for persisting audit events to the AUDIT_LOG table, gated by the
 * AUDIT_LOG_ENABLED system config (Y/N). Entity-CRUD writes are captured on the calling thread
 * (request-scoped context isn't available in the background) but the actual DB write is deferred
 * to a bounded executor and only submitted after the enclosing transaction commits, so auditing
 * never adds request latency and rolled-back changes never produce an audit row. Hard delete is
 * the one exception, written synchronously via {@link #logEntitySync}, because an irreversible
 * action can't be fire-and-forget.
 * <p>
 * {@code systemConfigService} is {@code @Lazy}: every {@code AbstractBaseService} subclass
 * (including {@code SystemConfigServiceImpl} itself) holds a reference to this writer, so an
 * eager dependency back on {@code SystemConfigServiceImpl} would form a cycle — and Spring Boot
 * disables circular-reference resolution by default. {@code @Lazy} injects a deferred proxy
 * instead, resolved on first actual use, which breaks the cycle at startup. Declared on an
 * explicit constructor (not Lombok's @RequiredArgsConstructor) since Lombok's field-to-parameter
 * annotation copying isn't reliable for @Lazy.
 */
@Slf4j
@Component
public class AuditLogWriter {

	private static final String AUDIT_LOG_CONFIG_KEY = "AUDIT_LOG_ENABLED";

	private final SystemConfigServiceImpl systemConfigService;
	private final AuditLogRepo auditLogRepo;
	private final Executor auditLogExecutor;

	public AuditLogWriter(@Lazy SystemConfigServiceImpl systemConfigService,
						   AuditLogRepo auditLogRepo,
						   @Qualifier("auditLogExecutor") Executor auditLogExecutor) {
		this.systemConfigService = systemConfigService;
		this.auditLogRepo = auditLogRepo;
		this.auditLogExecutor = auditLogExecutor;
	}

	public void logEntity(String action, String entityType, Long entityId, AuditOutcome outcome, String changedFields) {
		if (!enabled()) return;
		submitAfterCommit(capture(AuditCategory.ENTITY, action, entityType, entityId, currentActor(), outcome, changedFields));
	}

	public void logEntitySync(String action, String entityType, Long entityId, AuditOutcome outcome, String changedFields) {
		if (!enabled()) return;
		capture(AuditCategory.ENTITY, action, entityType, entityId, currentActor(), outcome, changedFields).run();
	}

	/** For security events fired in an already-authenticated context (logout, force-logout). */
	public void logSecurity(String action, AuditOutcome outcome) {
		logSecurity(action, currentActor(), outcome);
	}

	/**
	 * For security events with no established SecurityContext yet (login, OTP, password
	 * reset/set) — the actor is whichever email/username the request itself named, not
	 * necessarily one that ever authenticates.
	 */
	public void logSecurity(String action, String actorEmail, AuditOutcome outcome) {
		if (!enabled()) return;
		String actor = StringUtils.isNotBlank(actorEmail) ? actorEmail : "unknown";
		submitAfterCommit(capture(AuditCategory.SECURITY, action, null, null, actor, outcome, null));
	}

	private boolean enabled() {
		SystemConfig config = systemConfigService.findCachedByKey(AUDIT_LOG_CONFIG_KEY);
		return config != null && "Y".equalsIgnoreCase(config.getConfigValue());
	}

	private Runnable capture(AuditCategory category, String action, String entityType, Long entityId, String actor, AuditOutcome outcome, String changedFields) {
		String ip = RequestUtils.getClientTerminal();
		String userAgent = RequestUtils.getUserAgent();
		String uri = RequestUtils.getRequestUri();
		String method = RequestUtils.getHttpMethod();
		String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
		return () -> persist(category, action, entityType, entityId, actor, outcome, ip, userAgent, uri, method, correlationId, changedFields);
	}

	private void submitAfterCommit(Runnable persistTask) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					auditLogExecutor.execute(persistTask);
				}
			});
		} else {
			auditLogExecutor.execute(persistTask);
		}
	}

	private void persist(AuditCategory category, String action, String entityType, Long entityId, String actor,
						  AuditOutcome outcome, String ip, String userAgent, String uri, String method, String correlationId,
						  String changedFields) {
		try {
			Date now = new Date();
			AuditLog entry = new AuditLog()
					.setCategory(category)
					.setAction(action)
					.setEntityType(entityType)
					.setEntityId(entityId)
					.setOutcome(outcome)
					.setIpAddress(ip)
					.setUserAgent(userAgent)
					.setRequestUri(uri)
					.setHttpMethod(method)
					.setCorrelationId(correlationId)
					.setChangedFields(changedFields);
			entry.setCreatedBy(actor).setCreatedOn(now).setCreatedTerminal(ip)
					.setUpdatedBy(actor).setUpdatedOn(now).setUpdatedTerminal(ip)
					.setDeleted(false);
			auditLogRepo.save(entry);
		} catch (Exception persistEx) {
			log.error("Failed to persist audit log: category={} action={} entityType={} entityId={}: {}",
					category, action, entityType, entityId, persistEx.getMessage(), persistEx);
		}
	}

	private String currentActor() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return "system";
		Object principal = auth.getPrincipal();
		return principal instanceof MyUserDetail mud ? mud.getUsername() : auth.getName();
	}
}
