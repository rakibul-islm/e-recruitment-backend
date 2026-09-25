package com.bd.erecruitment.notification;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SseEmitterRegistry {

	private final Map<Long, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

	public SseEmitter register(Long userId) {
		SseEmitter emitter = new SseEmitter(0L);
		List<SseEmitter> emitters = emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
		emitters.add(emitter);

		Runnable remove = () -> {
			emitters.remove(emitter);
			if (emitters.isEmpty()) emittersByUser.remove(userId, emitters);
		};
		emitter.onCompletion(remove);
		emitter.onTimeout(remove);
		emitter.onError(e -> remove.run());

		return emitter;
	}

	public void push(Long userId, Object payload) {
		push(userId, "sync", payload);
	}

	public void push(Long userId, String eventName, Object payload) {
		List<SseEmitter> emitters = emittersByUser.get(userId);
		if (emitters == null || emitters.isEmpty()) return;

		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().name(eventName).data(payload));
			} catch (IOException | IllegalStateException e) {
				completeQuietly(emitter);
			}
		}
	}

	public void heartbeat() {
		emittersByUser.values().stream().flatMap(List::stream).forEach(emitter -> {
			try {
				emitter.send(SseEmitter.event().comment("ping"));
			} catch (IOException | IllegalStateException e) {
				completeQuietly(emitter);
			}
		});
	}

	private void completeQuietly(SseEmitter emitter) {
		try {
			emitter.complete();
		} catch (IllegalStateException ignored) {
		}
	}
}
