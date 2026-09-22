package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.res.NotificationPollResDTO;
import com.bd.erecruitment.dto.res.NotificationResDTO;
import com.bd.erecruitment.service.impl.NotificationServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestApiController
@RequestMapping("/notification")
@RequiredArgsConstructor
@Tag(name = "4.4 Notifications", description = "In-app notifications for the logged-in user")
public class NotificationController {

	private final NotificationServiceImpl notificationService;

	@Operation(summary = "Unread count and latest notification id, for polling")
	@GetMapping("/poll")
	public Response<NotificationPollResDTO> poll() {
		return notificationService.poll();
	}

	@Operation(summary = "Live stream of unread-count updates for the logged-in user (SSE)")
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream() {
		return notificationService.stream();
	}

	@Operation(summary = "My notifications, newest first; pass the last id received as beforeId to load older ones")
	@GetMapping("/my")
	public Response<NotificationResDTO> myList(
			@RequestParam(defaultValue = "9223372036854775807") Long beforeId,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "false") boolean unreadOnly) {
		return notificationService.myList(beforeId, size, unreadOnly);
	}

	@Operation(summary = "A single notification of the logged-in user")
	@GetMapping("/{id}")
	public Response<NotificationResDTO> findById(@PathVariable Long id) {
		return notificationService.findById(id);
	}

	@Operation(summary = "Mark one notification as read")
	@PutMapping("/{id}/read")
	public Response<NotificationResDTO> markRead(@PathVariable Long id) {
		return notificationService.markRead(id);
	}

	@Operation(summary = "Mark all my notifications as read")
	@PutMapping("/read-all")
	public Response<NotificationResDTO> markAllRead() {
		return notificationService.markAllRead();
	}

	@Operation(summary = "Remove a notification")
	@DeleteMapping("/{id}")
	public Response<NotificationResDTO> remove(@PathVariable Long id) {
		return notificationService.remove(id);
	}
}
