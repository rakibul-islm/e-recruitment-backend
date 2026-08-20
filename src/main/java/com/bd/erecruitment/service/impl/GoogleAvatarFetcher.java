package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleAvatarFetcher {

	private final UserRepo userRepo;
	private final RestTemplate restTemplate = buildRestTemplate();

	// Runs off the login request thread so a slow/large Google avatar download never delays sign-in.
	@Async
	public void fetchAndStore(Long userId, String pictureUrl) {
		try {
			byte[] avatar = restTemplate.getForObject(pictureUrl, byte[].class);
			userRepo.findByIdAndDeleted(userId, false).ifPresent(user -> {
				user.setFileData(avatar);
				userRepo.save(user);
			});
		} catch (Exception e) {
			log.warn("Failed to fetch Google avatar for user {}: {}", userId, e.getMessage());
		}
	}

	private static RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(8000);
		return new RestTemplate(factory);
	}
}
