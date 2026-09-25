package com.bd.erecruitment.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationHeartbeatScheduler {

	private final SseEmitterRegistry sseEmitterRegistry;

	@Scheduled(fixedRate = 20000)
	public void ping() {
		sseEmitterRegistry.heartbeat();
	}
}
