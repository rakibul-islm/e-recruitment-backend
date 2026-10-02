package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.audit.AuditAction;
import com.bd.erecruitment.audit.AuditExempt;
import com.bd.erecruitment.audit.AuditLogWriter;
import com.bd.erecruitment.dto.req.UserSessionReqDto;
import com.bd.erecruitment.dto.res.GuestSessionResDTO;
import com.bd.erecruitment.dto.res.SessionSummaryResDTO;
import com.bd.erecruitment.dto.res.UserSessionResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.entity.UserSession;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.notification.SseEmitterRegistry;
import com.bd.erecruitment.repository.UserSessionRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.specification.GenericSpecification;
import com.bd.erecruitment.util.ClientInfo;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AuditExempt
public class UserSessionServiceImpl extends AbstractBaseService<UserSession> implements UserSessionService, BaseService<UserSessionResDTO, UserSessionReqDto> {

	private static final String DEFAULT_LOGOUT_REASON = "force-logout";

	@Autowired private AuditLogWriter auditLogWriter;
	@Autowired private SseEmitterRegistry sseEmitterRegistry;

	private final UserSessionRepo userSessionRepo;

	private final Set<String> revokedJtiCache = ConcurrentHashMap.newKeySet();

	UserSessionServiceImpl(UserSessionRepo userSessionRepo) {
		super(userSessionRepo);
		this.userSessionRepo = userSessionRepo;
	}

	@PostConstruct
	private void preloadRevokedCache() {
		refreshRevokedCache();
	}

	@Scheduled(cron = "0 0 * * * *", zone = "Asia/Dhaka")
	public void refreshRevokedCache() {
		Set<String> fresh = new HashSet<>(userSessionRepo.findRevokedJtisNotExpired(new Date()));
		revokedJtiCache.retainAll(fresh);
		revokedJtiCache.addAll(fresh);
	}

	@Transactional
	@Override
	public UserSession createSession(User user, String jti, Date issuedAt, Date expiresAt) {
		ClientInfo client = RequestUtils.getClientInfo();
		UserSession session = UserSession.builder()
				.user(user)
				.jti(jti)
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.ipAddress(RequestUtils.getClientTerminal())
				.city(client.city())
				.country(client.country())
				.deviceType(client.deviceType())
				.os(client.os())
				.browser(client.browser())
				.revoked(false)
				.createdBy(user.getEmail())
				.createdOn(issuedAt)
				.createdLocation(client.location())
				.createdDevice(client.device())
				.createdUserAgent(RequestUtils.getClientUserAgent())
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
		return forceLogoutUser(userId, DEFAULT_LOGOUT_REASON);
	}

	@Transactional
	@Override
	public Response<Object> forceLogoutUser(Long userId, String reason) {
		List<UserSession> sessions = userSessionRepo.findAllByUser_IdAndRevokedFalse(userId);
		sessions.forEach(this::revoke);
		notifyForceLogout(userId, reason);
		auditLogWriter.logSecurity(AuditAction.FORCE_LOGOUT, AuditOutcome.SUCCESS);
		return getSuccessResponse("Logged out " + sessions.size() + " active session(s)");
	}

	@Transactional
	@Override
	public Response<Object> forceLogoutAll() {
		List<UserSession> sessions = userSessionRepo.findAllByRevokedFalseAndDeletedFalseAndExpiresAtAfter(new Date());
		sessions.forEach(this::revoke);
		sessions.stream().map(s -> s.getUser().getId()).distinct().forEach(this::notifyForceLogout);
		auditLogWriter.logSecurity(AuditAction.FORCE_LOGOUT, AuditOutcome.SUCCESS);
		return getSuccessResponse("Logged out " + sessions.size() + " active session(s)");
	}

	@Transactional
	@Override
	public Response<Object> logoutCurrentSession(String jti) {
		if (StringUtils.isNotBlank(jti)) userSessionRepo.findByJti(jti).ifPresent(this::revoke);
		return getSuccessResponse("Logged out successfully");
	}

	@Transactional
	@Override
	public Response<Object> updateCurrentSessionLocation(String jti) {
		ClientInfo client = RequestUtils.getClientInfo();
		if (StringUtils.isBlank(jti) || StringUtils.isAllBlank(client.city(), client.country())) return getSuccessResponse("No location to update");
		userSessionRepo.findByJti(jti).ifPresent(session -> {
			session.setCity(client.city()).setCountry(client.country()).setCreatedLocation(client.location());
			userSessionRepo.save(session);
		});
		return getSuccessResponse("Session location updated");
	}

	@Transactional
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
		return getSuccessResponse("Found", new SessionSummaryResDTO(activeSessions, sseEmitterRegistry.onlineUserCount(), sseEmitterRegistry.onlineGuestCount()));
	}

	@Transactional
	@Override
	public Response<UserSessionResDTO> getOnlineUsers() {
		List<Long> onlineUserIds = sseEmitterRegistry.onlineUserIds();
		if (onlineUserIds.isEmpty()) return getSuccessResponse("No data found", List.<UserSessionResDTO>of());
		List<UserSessionResDTO> dtos = latestPerUser(userSessionRepo.findActiveByUserIds(onlineUserIds, new Date()));
		return getSuccessResponse(dtos.isEmpty() ? "No data found" : "Found", dtos);
	}

	@Transactional
	@Override
	public Response<UserSessionResDTO> getActiveUsers() {
		List<UserSessionResDTO> dtos = latestPerUser(userSessionRepo.findAllActive(new Date()));
		return getSuccessResponse(dtos.isEmpty() ? "No data found" : "Found", dtos);
	}

	private List<UserSessionResDTO> latestPerUser(List<UserSession> sessionsNewestFirst) {
		Map<Long, UserSessionResDTO> latestByUser = new LinkedHashMap<>();
		sessionsNewestFirst.forEach(session -> latestByUser.compute(session.getUser().getId(), (id, existing) -> {
			UserSessionResDTO dto = existing != null ? existing : new UserSessionResDTO(session);
			dto.setSessionCount(dto.getSessionCount() == null ? 1L : dto.getSessionCount() + 1);
			return dto;
		}));
		return List.copyOf(latestByUser.values());
	}

	@Override
	public Response<GuestSessionResDTO> getActiveGuests() {
		List<GuestSessionResDTO> guests = sseEmitterRegistry.onlineGuests().stream()
				.map(g -> new GuestSessionResDTO(g.guestId(), g.ipAddress(), g.city(), g.country(), g.deviceType(), g.os(), g.browser(), g.connectedAt())).toList();
		return getSuccessResponse(guests.isEmpty() ? "No data found" : "Found", guests);
	}

	@Override
	public SseEmitter watchSummary() {
		SseEmitter emitter = sseEmitterRegistry.registerWatcher();
		sseEmitterRegistry.pushToWatchers(getSummary().getObj());
		return emitter;
	}

	@Transactional
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
		notifyForceLogout(session.getUser().getId());
		return getSuccessResponse("Session logged out successfully", new UserSessionResDTO(session));
	}

	@Transactional
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

	private void notifyForceLogout(Long userId) {
		notifyForceLogout(userId, DEFAULT_LOGOUT_REASON);
	}

	private void notifyForceLogout(Long userId, String reason) {
		sseEmitterRegistry.push(userId, "force-logout", Map.of("reason", reason));
	}
}
