package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.audit.AuditAction;
import com.bd.erecruitment.audit.AuditExempt;
import com.bd.erecruitment.audit.AuditLogWriter;
import com.bd.erecruitment.dto.req.UserSessionReqDto;
import com.bd.erecruitment.dto.res.SessionSummaryResDTO;
import com.bd.erecruitment.dto.res.UserSessionResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.entity.UserSession;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.repository.UserSessionRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.GuestSessionTracker;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.specification.GenericSpecification;
import com.bd.erecruitment.util.RequestUtils;
import com.bd.erecruitment.util.Response;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AuditExempt // session churn on every login shouldn't flood the entity-audit path; session events are covered explicitly instead (phase 2)
public class UserSessionServiceImpl extends AbstractBaseService<UserSession> implements UserSessionService, BaseService<UserSessionResDTO, UserSessionReqDto> {

	@Autowired private GuestSessionTracker guestSessionTracker;
	@Autowired private AuditLogWriter auditLogWriter;

	private final UserSessionRepo userSessionRepo;

	// Checked on every request; preloaded at startup, re-synced hourly, updated instantly by revoke().
	private final Set<String> revokedJtiCache = ConcurrentHashMap.newKeySet();

	UserSessionServiceImpl(UserSessionRepo userSessionRepo) {
		super(userSessionRepo);
		this.userSessionRepo = userSessionRepo;
	}

	@PostConstruct
	private void preloadRevokedCache() {
		refreshRevokedCache();
	}

	@Scheduled(cron = "0 0 * * * *")
	public void refreshRevokedCache() {
		Set<String> fresh = new HashSet<>(userSessionRepo.findRevokedJtisNotExpired(new Date()));
		revokedJtiCache.retainAll(fresh);
		revokedJtiCache.addAll(fresh);
	}

	@Transactional
	@Override
	public UserSession createSession(User user, String jti, Date issuedAt, Date expiresAt) {
		UserSession session = UserSession.builder()
				.user(user)
				.jti(jti)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.ipAddress(RequestUtils.getClientTerminal())
				.userAgent(RequestUtils.getUserAgent())
				.revoked(false)
				.createdBy(user.getEmail())
				.createdOn(issuedAt)
				.updatedBy(user.getEmail())
				.updatedOn(issuedAt)
				.deleted(false)
				.build();
		return userSessionRepo.save(session);
	}

	@Override
	public boolean isActive(String jti) {
		return !revokedJtiCache.contains(jti);
	}

	@Transactional
	@Override
	public Response<Object> forceLogoutUser(Long userId) {
		List<UserSession> sessions = userSessionRepo.findAllByUser_IdAndRevokedFalse(userId);
		sessions.forEach(this::revoke);
		auditLogWriter.logSecurity(AuditAction.FORCE_LOGOUT, AuditOutcome.SUCCESS);
		return getSuccessResponse("Logged out " + sessions.size() + " active session(s)");
	}

	@Transactional
	@Override
	public Response<Object> forceLogoutAll() {
		List<UserSession> sessions = userSessionRepo.findAllByRevokedFalseAndDeletedFalseAndExpiresAtAfter(new Date());
		sessions.forEach(this::revoke);
		auditLogWriter.logSecurity(AuditAction.FORCE_LOGOUT, AuditOutcome.SUCCESS);
		return getSuccessResponse("Logged out " + sessions.size() + " active session(s)");
	}

	@Transactional
	@Override
	public Response<Object> logoutCurrentSession(String jti) {
		if (StringUtils.isNotBlank(jti)) userSessionRepo.findByJti(jti).ifPresent(this::revoke);
		return getSuccessResponse("Logged out successfully");
	}

	@Override
	public Response<UserSessionResDTO> findByUser(Long userId) {
		List<UserSessionResDTO> dtos = userSessionRepo.findAllByUser_IdOrderByIdDesc(userId).stream()
				.map(UserSessionResDTO::new).toList();
		return getSuccessResponse(dtos.isEmpty() ? "No data found" : "Found", dtos);
	}

	@Override
	public Response<SessionSummaryResDTO> getSummary() {
		Date now = new Date();
		long activeSessions = userSessionRepo.countByRevokedFalseAndDeletedFalseAndExpiresAtAfter(now);
		long distinctActiveUsers = userSessionRepo.countDistinctActiveUsers(now);
		return getSuccessResponse("Found", new SessionSummaryResDTO(activeSessions, distinctActiveUsers, guestSessionTracker.activeCount()));
	}

	@Override
	public Response<UserSessionResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Found", new UserSessionResDTO(findByIdOrThrow(id, "Session not found")));
	}

	@Transactional
	@Override
	public Response<UserSessionResDTO> remove(Long id) {
		UserSession session = findByIdOrThrow(id, "Session not found");
		revoke(session);
		return getSuccessResponse("Session logged out successfully", new UserSessionResDTO(session));
	}

	// "status" (ACTIVE/REVOKED/EXPIRED) and "userEmail_like" aren't real UserSession columns —
	// status is a computed combination of revoked + expiresAt, and email lives on the related
	// User — so both are pulled out of the generic filter map and applied as extra predicates
	// instead of going through GenericSpecification's plain-column matching.
	@Override
	public Response<UserSessionResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		Map<String, String> remaining = new HashMap<>(filters);
		String status = remaining.remove("status");
		String userEmail = remaining.remove("userEmail_like");

		Specification<UserSession> spec = GenericSpecification.build(remaining);
		if (StringUtils.isNotBlank(status)) spec = spec.and(byStatus(status));
		if (StringUtils.isNotBlank(userEmail)) spec = spec.and(byUserEmail(userEmail));

		return genericFilter(spec, pageable, isPageable, UserSessionResDTO.class);
	}

	private Specification<UserSession> byStatus(String status) {
		Date now = new Date();
		return (root, query, cb) -> switch (status.toUpperCase()) {
			case "ACTIVE" -> cb.and(cb.isFalse(root.get("revoked")), cb.greaterThan(root.get("expiresAt"), now));
			case "REVOKED" -> cb.isTrue(root.get("revoked"));
			case "EXPIRED" -> cb.and(cb.isFalse(root.get("revoked")), cb.lessThanOrEqualTo(root.get("expiresAt"), now));
			default -> cb.conjunction();
		};
	}

	private Specification<UserSession> byUserEmail(String email) {
		return (root, query, cb) -> cb.like(cb.lower(root.join("user").get("email")), "%" + email.toLowerCase() + "%");
	}

	@Override
	public Response<UserSessionResDTO> save(UserSessionReqDto reqDto) { return null; }

	@Override
	public Response<UserSessionResDTO> update(UserSessionReqDto reqDto) { return null; }

	@Override
	public Response<UserSessionResDTO> delete(Long id) { return null; }

	private void revoke(UserSession session) {
		String actor = getLoggedInUserDetails() != null ? getLoggedInUserDetails().getUsername() : "system";
		session.setRevoked(true).setRevokedAt(new Date()).setRevokedBy(actor);
		userSessionRepo.save(session);
		revokedJtiCache.add(session.getJti());
	}
}
