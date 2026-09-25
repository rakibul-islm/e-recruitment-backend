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

@Slf4j
@Component
public class AuditLogWriter {

	private static final String AUDIT_LOG_CONFIG_KEY = "AUDIT_LOG_ENABLED";

	private final SystemConfigServiceImpl systemConfigService;
	private final AuditLogRepo auditLogRepo;
	private final Executor auditLogExecutor;

	// @Lazy breaks the AbstractBaseService -> AuditLogWriter -> SystemConfigServiceImpl cycle; explicit constructor because Lombok does not reliably copy it.
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

	public void logSecurity(String action, AuditOutcome outcome) {
		logSecurity(action, currentActor(), outcome);
	}

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
