package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.service.GuestSessionTracker;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class GuestSessionTrackerImpl implements GuestSessionTracker {

	private static final long ACTIVE_WINDOW_MS = 15 * 60 * 1000L;

	private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

	@Override
	public void track(String guestId) {
		lastSeen.put(guestId, System.currentTimeMillis());
	}

	@Override
	public long activeCount() {
		long cutoff = System.currentTimeMillis() - ACTIVE_WINDOW_MS;
		lastSeen.entrySet().removeIf(e -> e.getValue() < cutoff);
		return lastSeen.size();
	}
}
