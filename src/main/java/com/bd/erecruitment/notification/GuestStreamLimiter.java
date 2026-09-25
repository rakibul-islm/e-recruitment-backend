package com.bd.erecruitment.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GuestStreamLimiter {

	private static final String UNKNOWN_CLIENT = "unknown";

	private final int maxTotal;
	private final int maxPerClient;
	private final AtomicInteger total = new AtomicInteger();
	private final ConcurrentHashMap<String, Integer> perClient = new ConcurrentHashMap<>();

	public GuestStreamLimiter(
			@Value("${app.presence.max-guest-streams:2000}") int maxTotal,
			@Value("${app.presence.max-streams-per-client:50}") int maxPerClient) {
		this.maxTotal = maxTotal;
		this.maxPerClient = maxPerClient;
	}

	public Runnable tryAcquire(String clientKey) {
		String key = clientKey != null ? clientKey : UNKNOWN_CLIENT;
		if (total.incrementAndGet() > maxTotal) {
			total.decrementAndGet();
			return null;
		}

		boolean[] allowed = {false};
		perClient.compute(key, (k, count) -> {
			int current = count != null ? count : 0;
			if (current >= maxPerClient) return count;
			allowed[0] = true;
			return current + 1;
		});
		if (!allowed[0]) {
			total.decrementAndGet();
			return null;
		}

		AtomicBoolean released = new AtomicBoolean();
		return () -> {
			if (!released.compareAndSet(false, true)) return;
			total.decrementAndGet();
			perClient.computeIfPresent(key, (k, count) -> count <= 1 ? null : count - 1);
		};
	}
}
