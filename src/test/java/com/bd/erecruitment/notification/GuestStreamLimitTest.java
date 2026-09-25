package com.bd.erecruitment.notification;

import com.bd.erecruitment.controller.PresenceController;
import com.bd.erecruitment.security.GuestTrackingInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class GuestStreamLimitTest {

	private final SseEmitterRegistry registry = new SseEmitterRegistry(event -> { });

	@Test
	void limiterRejectsBeyondTheTotalAndAcceptsAgainAfterARelease() {
		GuestStreamLimiter limiter = new GuestStreamLimiter(2, 10);

		Runnable first = limiter.tryAcquire("a");
		assertThat(limiter.tryAcquire("b")).isNotNull();
		assertThat(limiter.tryAcquire("c")).isNull();

		first.run();
		first.run();

		assertThat(limiter.tryAcquire("c")).isNotNull();
		assertThat(limiter.tryAcquire("d")).isNull();
	}

	@Test
	void limiterRejectsBeyondThePerClientCapButOtherClientsStillGetIn() {
		GuestStreamLimiter limiter = new GuestStreamLimiter(100, 2);

		assertThat(limiter.tryAcquire("a")).isNotNull();
		assertThat(limiter.tryAcquire("a")).isNotNull();
		assertThat(limiter.tryAcquire("a")).isNull();
		assertThat(limiter.tryAcquire("b")).isNotNull();
	}

	@Test
	void controllerAnswers429OverThePerClientCapAndFreesTheSlotWhenAStreamCloses() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PresenceController(registry, new GuestStreamLimiter(100, 2))).build();

		MvcResult first = open(mockMvc, "guest-1", "10.0.0.1");
		open(mockMvc, "guest-2", "10.0.0.1");
		assertThat(open(mockMvc, "guest-3", "10.0.0.1").getResponse().getStatus()).isEqualTo(429);
		assertThat(open(mockMvc, "guest-4", "10.0.0.2").getResponse().getStatus()).isEqualTo(200);
		assertThat(registry.onlineGuestCount()).isEqualTo(3);

		first.getRequest().getAsyncContext().complete();

		assertThat(open(mockMvc, "guest-5", "10.0.0.1").getResponse().getStatus()).isEqualTo(200);
		assertThat(registry.onlineGuestCount()).isEqualTo(3);
	}

	@Test
	void controllerAnswers429OverTheTotalCapEvenFromNewClients() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new PresenceController(registry, new GuestStreamLimiter(2, 10))).build();

		open(mockMvc, "guest-1", "10.0.0.1");
		open(mockMvc, "guest-2", "10.0.0.2");

		assertThat(open(mockMvc, "guest-3", "10.0.0.3").getResponse().getStatus()).isEqualTo(429);
		assertThat(registry.onlineGuestCount()).isEqualTo(2);
	}

	private MvcResult open(MockMvc mockMvc, String guestId, String ip) throws Exception {
		return mockMvc.perform(get("/presence/guest-stream")
				.requestAttr(GuestTrackingInterceptor.GUEST_ID_ATTRIBUTE, guestId)
				.with(request -> {
					request.setRemoteAddr(ip);
					return request;
				})).andReturn();
	}
}
