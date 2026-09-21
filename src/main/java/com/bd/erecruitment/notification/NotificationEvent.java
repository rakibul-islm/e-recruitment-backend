package com.bd.erecruitment.notification;

import com.bd.erecruitment.enums.NotificationType;

import java.util.LinkedHashMap;
import java.util.Map;

public record NotificationEvent(NotificationType type, Long userId, String email, String authority, String actionRoute,
		String dedupeKey, Map<String, Object> params) {

	public static NotificationEvent toUser(Long userId, NotificationType type, String actionRoute, Object... keyValues) {
		return new NotificationEvent(type, userId, null, null, actionRoute, null, toParams(keyValues));
	}

	public static NotificationEvent toEmail(String email, NotificationType type, String actionRoute, Object... keyValues) {
		return new NotificationEvent(type, null, email, null, actionRoute, null, toParams(keyValues));
	}

	public static NotificationEvent toAuthority(String authority, NotificationType type, String actionRoute, Object... keyValues) {
		return new NotificationEvent(type, null, null, authority, actionRoute, null, toParams(keyValues));
	}

	public NotificationEvent withDedupeKey(String key) {
		return new NotificationEvent(type, userId, email, authority, actionRoute, key, params);
	}

	private static Map<String, Object> toParams(Object[] keyValues) {
		Map<String, Object> params = new LinkedHashMap<>();
		for (int i = 0; i + 1 < keyValues.length; i += 2) {
			if (keyValues[i + 1] != null) params.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
		}
		return params;
	}
}
