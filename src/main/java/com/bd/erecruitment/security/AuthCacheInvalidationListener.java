package com.bd.erecruitment.security;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

/** JPA entity listener for User, Role and Permission: any change drops the cached login details (see {@link UserDetailsCache}). */
public class AuthCacheInvalidationListener {

	@PostPersist
	@PostUpdate
	@PostRemove
	public void onChange(Object entity) {
		UserDetailsCache.clearAfterCommit();
	}
}
