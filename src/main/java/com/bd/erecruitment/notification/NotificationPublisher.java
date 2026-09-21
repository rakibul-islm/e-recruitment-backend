package com.bd.erecruitment.notification;

import com.bd.erecruitment.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

	private final ApplicationEventPublisher eventPublisher;

	public void notifyUser(Long userId, NotificationType type, String actionRoute, Object... keyValues) {
		eventPublisher.publishEvent(NotificationEvent.toUser(userId, type, actionRoute, keyValues));
	}

	public void notifyEmail(String email, NotificationType type, String actionRoute, Object... keyValues) {
		eventPublisher.publishEvent(NotificationEvent.toEmail(email, type, actionRoute, keyValues));
	}

	public void notifyAuthority(String authority, NotificationType type, String actionRoute, Object... keyValues) {
		eventPublisher.publishEvent(NotificationEvent.toAuthority(authority, type, actionRoute, keyValues));
	}
}
