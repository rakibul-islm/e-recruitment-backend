package com.bd.erecruitment.notification;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationHeartbeatScheduler {

	private static final long HEARTBEAT_SECONDS = 5;
	private static final long PRESENCE_FLUSH_MILLIS = 1000;
	private static final long PRESENCE_REFRESH_SECONDS = 30;

	private final SseEmitterRegistry sseEmitterRegistry;
	private final PresenceBroadcaster presenceBroadcaster;

	private ScheduledExecutorService executor;

	@PostConstruct
	void start() {
		executor = Executors.newScheduledThreadPool(2, runnable -> {
			Thread thread = new Thread(runnable, "sse-keepalive");
			thread.setDaemon(true);
			return thread;
		});
		executor.scheduleWithFixedDelay(guarded(sseEmitterRegistry::heartbeat), HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
		executor.scheduleWithFixedDelay(guarded(presenceBroadcaster::flushIfChanged), PRESENCE_FLUSH_MILLIS, PRESENCE_FLUSH_MILLIS, TimeUnit.MILLISECONDS);
		executor.scheduleWithFixedDelay(guarded(presenceBroadcaster::broadcast), PRESENCE_REFRESH_SECONDS, PRESENCE_REFRESH_SECONDS, TimeUnit.SECONDS);
	}

	@PreDestroy
	void stop() {
		executor.shutdownNow();
	}

	private Runnable guarded(Runnable task) {
		return () -> {
			try {
				task.run();
			} catch (RuntimeException e) {
				log.warn("SSE keep-alive task failed", e);
			}
		};
	}
}
