package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.exception.ExceptionLogWriter;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

// Split out from NotificationBroadcastServiceImpl so @Async actually applies (a method can't be
// proxied via self-invocation from within the same class) - mirrors NotificationPublisher/NotificationListener's
// trigger/async-worker split.
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminMessageEmailSender {

	private final UserRepo userRepo;
	private final MailService mailService;
	private final ExceptionLogWriter exceptionLogWriter;

	@Async("notificationExecutor")
	public void sendAll(List<Long> recipientIds, String title, String message) {
		userRepo.findAllById(recipientIds).forEach(user -> {
			try {
				mailService.sendAdminMessageEmail(user.getEmail(), user.getFullName(), title, message);
			} catch (Exception e) {
				log.warn("Failed to send admin-message email to {}: {}", user.getEmail(), e.getMessage());
				exceptionLogWriter.log(e, 0, e.getMessage(), "AdminMessageEmailSender.sendAll:" + user.getEmail());
			}
		});
	}
}
