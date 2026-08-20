package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.UserSessionReqDto;
import com.bd.erecruitment.dto.res.SessionSummaryResDTO;
import com.bd.erecruitment.dto.res.UserSessionResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.entity.UserSession;
import com.bd.erecruitment.repository.UserSessionRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.GuestSessionTracker;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.util.RequestUtils;
import com.bd.erecruitment.util.Response;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionServiceImpl extends AbstractBaseService<UserSession> implements UserSessionService, BaseService<UserSessionResDTO, UserSessionReqDto> {

	@Autowired private GuestSessionTracker guestSessionTracker;

	private final UserSessionRepo userSessionRepo;

	// In-memory revocation cache checked on every authenticated request; preloaded on
	// startup and re-synced hourly so it self-heals across restarts, while revoke()
	// updates it immediately so force-logout takes effect without waiting for the sync.
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
		return getSuccessResponse("Logged out " + sessions.size() + " active session(s)");
	}

	@Transactional
	@Override
	public Response<Object> forceLogoutAll() {
		List<UserSession> sessions = userSessionRepo.findAllByRevokedFalseAndDeletedFalseAndExpiresAtAfter(new Date());
		sessions.forEach(this::revoke);
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

	@Override
	public Response<UserSessionResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, UserSessionResDTO.class);
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
