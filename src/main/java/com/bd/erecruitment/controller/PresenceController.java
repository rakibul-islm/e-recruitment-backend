package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.notification.GuestStreamLimiter;
import com.bd.erecruitment.notification.SseEmitterRegistry;
import com.bd.erecruitment.security.GuestTrackingInterceptor;
import com.bd.erecruitment.util.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestApiController
@RequestMapping("/presence")
@RequiredArgsConstructor
@Tag(name = "Presence", description = "Marks a visitor as online while their stream is open")
public class PresenceController {

	private final SseEmitterRegistry sseEmitterRegistry;
	private final GuestStreamLimiter guestStreamLimiter;

	@Operation(summary = "Open-stream presence for a guest (no login); the visitor counts as online until the stream closes")
	@GetMapping(value = "/guest-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> guestStream(
			@RequestAttribute(name = GuestTrackingInterceptor.GUEST_ID_ATTRIBUTE, required = false) String guestId,
			HttpServletRequest request) {
		Runnable release = guestStreamLimiter.tryAcquire(RequestUtils.getClientTerminal(request));
		if (release == null) return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();

		SseEmitter emitter = sseEmitterRegistry.registerGuest(guestId != null ? guestId : UUID.randomUUID().toString(), release);
		return ResponseEntity.ok(emitter);
	}
}
