package com.bd.erecruitment.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived cache of the logged-in user's {@link UserDetails} (roles + permissions) for the JWT filter, so a page that
 * fires many requests at once does one user/role/permission join instead of one per request. Login and credential
 * checks keep calling the service directly and never see this cache.
 * <p>
 * Entries expire after a few seconds and the whole cache is cleared, after commit, whenever a user, role or permission
 * changes (see {@link AuthCacheInvalidationListener}), so a role change, lock or deactivation takes effect on the next
 * request rather than after the TTL.
 */
@Component
public class UserDetailsCache {

	private record Entry(UserDetails details, long expiresAtNanos) {
	}

	private static volatile UserDetailsCache instance;

	private final Map<String, Entry> entries = new ConcurrentHashMap<>();
	private final long ttlNanos;

	public UserDetailsCache(@Value("${app.auth.user-cache-ttl-seconds:15}") long ttlSeconds) {
		this.ttlNanos = ttlSeconds * 1_000_000_000L;
		instance = this;
	}

	public UserDetails get(String username, UserDetailsService loader) {
		if (ttlNanos <= 0) return loader.loadUserByUsername(username);

		Entry entry = entries.get(username);
		long now = System.nanoTime();
		if (entry != null && now - entry.expiresAtNanos() < 0) return entry.details();

		UserDetails loaded = loader.loadUserByUsername(username);
		if (loaded != null) entries.put(username, new Entry(loaded, now + ttlNanos));
		return loaded;
	}

	public void clear() {
		entries.clear();
	}

	/** Clears the cache once the current transaction commits (or immediately when there is none). */
	public static void clearAfterCommit() {
		UserDetailsCache cache = instance;
		if (cache == null) return;
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					cache.clear();
				}
			});
		} else {
			cache.clear();
		}
	}
}
