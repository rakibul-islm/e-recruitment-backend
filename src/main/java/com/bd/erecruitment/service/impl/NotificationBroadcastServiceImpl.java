package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.NotificationBroadcastReqDto;
import com.bd.erecruitment.dto.res.RoleOptionResDTO;
import com.bd.erecruitment.dto.res.UserOptionResDTO;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.notification.NotificationPublisher;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.util.Response;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationBroadcastServiceImpl extends CommonFunctionsImpl {

	private static final int MAX_TITLE_LENGTH = 150;
	private static final int MAX_MESSAGE_LENGTH = 1000;
	private static final int USER_SEARCH_LIMIT = 20;

	private final RoleRepo roleRepo;
	private final UserRepo userRepo;
	private final NotificationPublisher notificationPublisher;
	private final AdminMessageEmailSender adminMessageEmailSender;

	public Response<RoleOptionResDTO> listRoles() {
		List<RoleOptionResDTO> roles = roleRepo.findByDeletedFalse().stream()
			.map(role -> new RoleOptionResDTO(role.getId(), role.getName()))
			.toList();
		return getSuccessResponse(roles.isEmpty() ? "No data found" : "Found", roles);
	}

	public Response<UserOptionResDTO> searchUsers(String keyword) {
		if (StringUtils.isBlank(keyword)) return getSuccessResponse("Found", List.<UserOptionResDTO>of());

		List<UserOptionResDTO> users = userRepo.searchActiveByKeyword(keyword.trim(), PageRequest.of(0, USER_SEARCH_LIMIT)).stream()
			.map(user -> new UserOptionResDTO(user.getId(), user.getFullName(), user.getEmail()))
			.toList();
		return getSuccessResponse(users.isEmpty() ? "No data found" : "Found", users);
	}

	public Response<Void> send(NotificationBroadcastReqDto req) {
		if (StringUtils.isBlank(req.getTitle()) || StringUtils.isBlank(req.getMessage())) {
			returnErrorException("Title and message are required");
		}
		if (req.getTargetType() == null) {
			returnErrorException("Target type is required");
		}

		String title = StringUtils.abbreviate(req.getTitle().trim(), MAX_TITLE_LENGTH);
		String message = StringUtils.abbreviate(req.getMessage().trim(), MAX_MESSAGE_LENGTH);

		List<Long> recipientIds = resolveRecipientIds(req);
		if (recipientIds.isEmpty()) {
			returnErrorException("No matching recipients found");
		}

		recipientIds.forEach(id -> notificationPublisher.notifyUser(id, NotificationType.ADMIN_MESSAGE, null, "title", title, "message", message));

		if (req.isSendEmail()) adminMessageEmailSender.sendAll(recipientIds, title, message);

		return getSuccessResponse("Notification sent to " + recipientIds.size() + " user(s)");
	}

	private List<Long> resolveRecipientIds(NotificationBroadcastReqDto req) {
		return switch (req.getTargetType()) {
			case ALL -> userRepo.findAllActiveIds();
			case ROLE -> {
				if (req.getRoleId() == null) throw new BadRequestException("Role is required");
				yield userRepo.findActiveIdsByRoleId(req.getRoleId());
			}
			case USER -> {
				if (req.getUserIds() == null || req.getUserIds().isEmpty()) throw new BadRequestException("At least one user is required");
				yield req.getUserIds().stream().distinct().toList();
			}
		};
	}
}
