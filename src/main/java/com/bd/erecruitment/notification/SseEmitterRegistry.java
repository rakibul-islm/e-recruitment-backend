package com.bd.erecruitment.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
public class SseEmitterRegistry {

	private static final int MAX_EMITTERS_PER_KEY = 5;

	private final ApplicationEventPublisher eventPublisher;

	private final EmitterGroup<Long> users = new EmitterGroup<>(true);
	private final EmitterGroup<String> guests = new EmitterGroup<>(true);
	private final EmitterGroup<String> watchers = new EmitterGroup<>(false);

	public SseEmitter register(Long userId) {
		return users.register(userId, () -> { });
	}

	public SseEmitter registerGuest(String guestId, Runnable onClose) {
		return guests.register(guestId, onClose);
	}

	public SseEmitter registerWatcher() {
		return watchers.register(UUID.randomUUID().toString(), () -> { });
	}

	public void push(Long userId, Object payload) {
		push(userId, "sync", payload);
	}

	public void push(Long userId, String eventName, Object payload) {
		users.push(userId, eventName, payload);
	}

	public void pushToWatchers(Object payload) {
		watchers.pushAll("presence", payload);
	}

	public long onlineUserCount() {
		return users.count();
	}

	public long onlineGuestCount() {
		return guests.count();
	}

	public boolean hasWatchers() {
		return watchers.count() > 0;
	}

	public void heartbeat() {
		users.heartbeat();
		guests.heartbeat();
		watchers.heartbeat();
	}

	private class EmitterGroup<K> {

		private final Map<K, List<SseEmitter>> emittersByKey = new ConcurrentHashMap<>();
		private final boolean announcePresence;

		EmitterGroup(boolean announcePresence) {
			this.announcePresence = announcePresence;
		}

		SseEmitter register(K key, Runnable onClose) {
			SseEmitter emitter = new SseEmitter(0L);
			List<SseEmitter> evicted = new ArrayList<>();
			boolean[] firstOfKey = {false};

			emittersByKey.compute(key, (k, current) -> {
				List<SseEmitter> emitters = current != null ? current : new CopyOnWriteArrayList<>();
				firstOfKey[0] = emitters.isEmpty();
				emitters.add(emitter);
				while (emitters.size() > MAX_EMITTERS_PER_KEY) evicted.add(emitters.remove(0));
				return emitters;
			});

			Runnable remove = () -> {
				remove(key, emitter);
				onClose.run();
			};
			emitter.onCompletion(remove);
			emitter.onTimeout(remove);
			emitter.onError(e -> remove.run());

			evicted.forEach(SseEmitterRegistry.this::completeQuietly);
			sendComment(emitter, "connected");
			if (firstOfKey[0]) announce();
			return emitter;
		}

		void push(K key, String eventName, Object payload) {
			List<SseEmitter> emitters = emittersByKey.get(key);
			if (emitters != null) emitters.forEach(emitter -> send(emitter, eventName, payload));
		}

		void pushAll(String eventName, Object payload) {
			emittersByKey.values().forEach(emitters -> emitters.forEach(emitter -> send(emitter, eventName, payload)));
		}

		void heartbeat() {
			emittersByKey.values().forEach(emitters -> emitters.forEach(emitter -> sendComment(emitter, "ping")));
		}

		long count() {
			return emittersByKey.size();
		}

		private void sendComment(SseEmitter emitter, String comment) {
			try {
				emitter.send(SseEmitter.event().comment(comment));
			} catch (IOException | IllegalStateException e) {
				completeQuietly(emitter);
			}
		}

		private void send(SseEmitter emitter, String eventName, Object payload) {
			try {
				emitter.send(SseEmitter.event().name(eventName).data(payload));
			} catch (IOException | IllegalStateException e) {
				completeQuietly(emitter);
			}
		}

		private void remove(K key, SseEmitter emitter) {
			boolean[] keyGone = {false};
			emittersByKey.computeIfPresent(key, (k, emitters) -> {
				emitters.remove(emitter);
				keyGone[0] = emitters.isEmpty();
				return keyGone[0] ? null : emitters;
			});
			if (keyGone[0]) announce();
		}

		private void announce() {
			if (announcePresence) eventPublisher.publishEvent(new PresenceChangedEvent());
		}
	}

	private void completeQuietly(SseEmitter emitter) {
		try {
			emitter.complete();
		} catch (IllegalStateException ignored) {
		}
	}
}
