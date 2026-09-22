package com.bd.erecruitment.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// Keeps intermediary proxies/load balancers from closing idle SSE connections, and prunes dead
// emitters (a failed send during the ping completes and deregisters that emitter).
@Component
@RequiredArgsConstructor
public class NotificationHeartbeatScheduler {

	private final SseEmitterRegistry sseEmitterRegistry;

	@Scheduled(fixedRate = 20000)
	public void ping() {
		sseEmitterRegistry.heartbeat();
	}
}
