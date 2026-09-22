package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.NotificationBroadcastReqDto;
import com.bd.erecruitment.dto.res.RoleOptionResDTO;
import com.bd.erecruitment.dto.res.UserOptionResDTO;
import com.bd.erecruitment.service.impl.NotificationBroadcastServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestApiController
@RequestMapping("/notification-broadcast")
@RequiredArgsConstructor
@Tag(name = "4.5 Notification Broadcast", description = "Admin-composed notifications sent to all users, a role, or one user")
public class NotificationBroadcastController {

	private final NotificationBroadcastServiceImpl notificationBroadcastService;

	@Operation(summary = "Roles available as a broadcast target")
	@GetMapping("/roles")
	public Response<RoleOptionResDTO> roles() {
		return notificationBroadcastService.listRoles();
	}

	@Operation(summary = "Search users by name or email for the specific-user target")
	@GetMapping("/users")
	public Response<UserOptionResDTO> users(@RequestParam(required = false) String keyword) {
		return notificationBroadcastService.searchUsers(keyword);
	}

	@Operation(summary = "Send a notification to all users, a role, or one user, optionally also by email")
	@PostMapping
	public Response<Void> send(@RequestBody NotificationBroadcastReqDto req) {
		return notificationBroadcastService.send(req);
	}
}
