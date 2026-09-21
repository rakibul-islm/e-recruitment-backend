package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.NotificationPollResDTO;
import com.bd.erecruitment.dto.res.NotificationResDTO;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.exception.UnauthorizedException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.notification.NotificationEvent;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.util.Response;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends CommonFunctionsImpl {

	private static final String SYSTEM = "system";
	private static final String SUPER_ADMIN = "SUPER_ADMIN";
	private static final int MAX_PAGE_SIZE = 50;
	private static final int MAX_PARAM_LENGTH = 200;

	private final NotificationRepo notificationRepo;
	private final UserRepo userRepo;
	private final ObjectMapper paramsMapper = new ObjectMapper();

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void create(NotificationEvent event) {
		Date now = new Date();
		for (Long recipientId : resolveRecipientIds(event)) {
			if (event.dedupeKey() != null && notificationRepo.existsByRecipientUserIdAndDedupeKey(recipientId, event.dedupeKey())) continue;

			Notification notification = new Notification()
				.setRecipientUserId(recipientId)
				.setType(event.type())
				.setActionRoute(safeRoute(event.actionRoute()))
				.setParamsJson(toJson(event.params()))
				.setDedupeKey(event.dedupeKey());
			notification.setCreatedBy(SYSTEM).setCreatedOn(now).setUpdatedBy(SYSTEM).setUpdatedOn(now).setDeleted(false);
			notificationRepo.save(notification);
		}
	}

	public Response<NotificationPollResDTO> poll() {
		NotificationRepo.PollSummary summary = notificationRepo.summarize(currentUser().getId());
		return getSuccessResponse("OK", new NotificationPollResDTO(summary.getUnreadCount(), summary.getLatestId(), new Date()));
	}

	public Response<NotificationResDTO> myList(Long beforeId, int size, boolean unreadOnly) {
		Long userId = currentUser().getId();
		PageRequest page = PageRequest.of(0, Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
		List<Notification> rows = unreadOnly
			? notificationRepo.findByRecipientUserIdAndDeletedAndReadOnIsNullAndIdLessThanOrderByIdDesc(userId, false, beforeId, page)
			: notificationRepo.findByRecipientUserIdAndDeletedAndIdLessThanOrderByIdDesc(userId, false, beforeId, page);
		List<NotificationResDTO> list = rows.stream().map(this::toDto).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	@Transactional
	public Response<NotificationResDTO> markRead(Long id) {
		Notification notification = findOwned(id);
		if (notification.getReadOn() == null) {
			touch(notification).setReadOn(notification.getUpdatedOn());
			notificationRepo.save(notification);
		}
		return getSuccessResponse("Notification marked as read", toDto(notification));
	}

	@Transactional
	public Response<NotificationResDTO> markAllRead() {
		MyUserDetail user = currentUser();
		notificationRepo.markAllRead(user.getId(), new Date(), user.getUsername());
		return getSuccessResponse("All notifications marked as read");
	}

	@Transactional
	public Response<NotificationResDTO> remove(Long id) {
		Notification notification = findOwned(id);
		touch(notification).setDeleted(true);
		notificationRepo.save(notification);
		return getSuccessResponse("Notification removed");
	}

	private Notification findOwned(Long id) {
		return notificationRepo.findByIdAndRecipientUserIdAndDeleted(id, currentUser().getId(), false)
			.orElseThrow(() -> new NotFoundException("Notification not found"));
	}

	private Notification touch(Notification notification) {
		notification.setUpdatedBy(currentUser().getUsername()).setUpdatedOn(new Date());
		return notification;
	}

	private List<Long> resolveRecipientIds(NotificationEvent event) {
		if (event.userId() != null) return List.of(event.userId());
		if (StringUtils.isNotBlank(event.authority())) return userRepo.findActiveIdsByAnyAuthority(Set.of(event.authority(), SUPER_ADMIN));
		if (StringUtils.isBlank(event.email())) return List.of();
		User user = userRepo.findByEmail(event.email());
		return user != null && !user.isDeleted() ? List.of(user.getId()) : List.of();
	}

	private String safeRoute(String route) {
		return route != null && route.startsWith("/") && !route.startsWith("//") ? route : null;
	}

	private String toJson(Map<String, Object> params) {
		Map<String, Object> trimmed = new LinkedHashMap<>();
		params.forEach((key, value) -> trimmed.put(key, value instanceof String text ? StringUtils.abbreviate(text, MAX_PARAM_LENGTH) : value));
		try {
			return paramsMapper.writeValueAsString(trimmed);
		} catch (JsonProcessingException e) {
			return "{}";
		}
	}

	private Map<String, Object> fromJson(String json) {
		if (StringUtils.isBlank(json)) return Map.of();
		try {
			return paramsMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
		} catch (JsonProcessingException e) {
			return Map.of();
		}
	}

	private NotificationResDTO toDto(Notification notification) {
		NotificationResDTO dto = new NotificationResDTO();
		dto.setId(notification.getId());
		dto.setType(notification.getType());
		dto.setActionRoute(notification.getActionRoute());
		dto.setParams(fromJson(notification.getParamsJson()));
		dto.setRead(notification.getReadOn() != null);
		dto.setCreatedOn(notification.getCreatedOn());
		return dto;
	}

	private MyUserDetail currentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.getPrincipal() instanceof MyUserDetail user) return user;
		throw new UnauthorizedException("Not authenticated");
	}
}
