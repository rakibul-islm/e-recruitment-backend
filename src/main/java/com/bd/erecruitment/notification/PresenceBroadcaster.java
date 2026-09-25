package com.bd.erecruitment.notification;

import com.bd.erecruitment.service.UserSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class PresenceBroadcaster {

	private final SseEmitterRegistry sseEmitterRegistry;
	private final UserSessionService userSessionService;

	private final AtomicBoolean changed = new AtomicBoolean();

	@EventListener
	public void onPresenceChanged(PresenceChangedEvent event) {
		changed.set(true);
	}

	public void flushIfChanged() {
		if (changed.getAndSet(false)) broadcast();
	}

	public void broadcast() {
		if (!sseEmitterRegistry.hasWatchers()) return;
		sseEmitterRegistry.pushToWatchers(userSessionService.getSummary().getObj());
	}
}
