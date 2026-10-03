package com.bd.erecruitment.notification;

import com.bd.erecruitment.entity.DeviceToken;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.exception.ExceptionLogWriter;
import com.bd.erecruitment.repository.DeviceTokenRepo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PushNotificationSender {

	private static final String CHANNEL_ID = "general";

	private final DeviceTokenRepo deviceTokenRepo;
	private final PushMessageComposer composer;
	private final ExceptionLogWriter exceptionLogWriter;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.fcm.service-account-json:}")
	private String serviceAccountJson;

	private boolean enabled;

	@PostConstruct
	void init() {
		if (StringUtils.isBlank(serviceAccountJson)) {
			log.info("FIREBASE_SERVICE_ACCOUNT_JSON is not set; push notifications are disabled");
			return;
		}
		try {
			byte[] credentials = decode(serviceAccountJson.trim());
			FirebaseOptions options = FirebaseOptions.builder()
				.setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(credentials)))
				.build();
			if (FirebaseApp.getApps().isEmpty()) FirebaseApp.initializeApp(options);
			enabled = true;
		} catch (Exception e) {
			log.warn("Push notifications disabled, Firebase could not be initialised: {}", e.getMessage());
		}
	}

	@Async("notificationExecutor")
	@Transactional
	public void send(Notification notification) {
		if (!enabled) return;
		try {
			List<DeviceToken> tokens = deviceTokenRepo.findByUserId(notification.getRecipientUserId());
			if (tokens.isEmpty()) return;
			PushMessageComposer.PushMessage text = composer.compose(notification.getType(), params(notification));
			tokens.forEach(token -> sendTo(token, notification, text));
		} catch (Exception e) {
			log.warn("Failed to push {} notification: {}", notification.getType(), e.getMessage());
			exceptionLogWriter.log(e, 0, e.getMessage(), "PushNotificationSender.send:" + notification.getType());
		}
	}

	private void sendTo(DeviceToken token, Notification notification, PushMessageComposer.PushMessage text) {
		Message message = Message.builder()
			.setToken(token.getPushToken())
			.setNotification(com.google.firebase.messaging.Notification.builder().setTitle(text.title()).setBody(text.body()).build())
			.putData("notificationId", String.valueOf(notification.getId()))
			.putData("route", StringUtils.defaultString(notification.getActionRoute()))
			.setAndroidConfig(AndroidConfig.builder()
				.setPriority(AndroidConfig.Priority.HIGH)
				.setNotification(AndroidNotification.builder().setChannelId(CHANNEL_ID).build())
				.build())
			.build();
		try {
			FirebaseMessaging.getInstance().send(message);
		} catch (FirebaseMessagingException e) {
			if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
				deviceTokenRepo.deleteByPushToken(token.getPushToken());
			} else {
				log.warn("FCM send failed ({}): {}", e.getMessagingErrorCode(), e.getMessage());
			}
		}
	}

	private Map<String, Object> params(Notification notification) {
		if (StringUtils.isBlank(notification.getParamsJson())) return Map.of();
		try {
			return objectMapper.readValue(notification.getParamsJson(), new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			return Map.of();
		}
	}

	private byte[] decode(String value) {
		return value.startsWith("{") ? value.getBytes(StandardCharsets.UTF_8) : Base64.getDecoder().decode(value);
	}
}
