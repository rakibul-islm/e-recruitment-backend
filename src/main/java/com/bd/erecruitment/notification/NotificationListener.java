package com.bd.erecruitment.notification;

import com.bd.erecruitment.service.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

	private final NotificationServiceImpl notificationService;

	@Async("notificationExecutor")
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void on(NotificationEvent event) {
		try {
			notificationService.create(event);
		} catch (Exception e) {
			log.warn("Failed to create {} notification: {}", event.type(), e.getMessage());
		}
	}
}
