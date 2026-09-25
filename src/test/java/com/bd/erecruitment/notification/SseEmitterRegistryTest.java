package com.bd.erecruitment.notification;

import com.bd.erecruitment.controller.PresenceController;
import com.bd.erecruitment.security.GuestTrackingInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class SseEmitterRegistryTest {

	private final List<Object> published = new ArrayList<>();

	private SseEmitterRegistry registry;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		registry = new SseEmitterRegistry(published::add);
		mockMvc = MockMvcBuilders.standaloneSetup(new PresenceController(registry, new GuestStreamLimiter(1000, 1000))).build();
	}

	@Test
	void guestCountsOncePerGuestNoMatterHowManyTabs() throws Exception {
		connect("guest-1");
		connect("guest-1");
		connect("guest-2");

		assertThat(registry.onlineGuestCount()).isEqualTo(2);
	}

	@Test
	void guestLeavesOnlyWhenTheirLastStreamCloses() throws Exception {
		MvcResult firstTab = connect("guest-1");
		MvcResult secondTab = connect("guest-1");
		published.clear();

		firstTab.getRequest().getAsyncContext().complete();
		assertThat(registry.onlineGuestCount()).isEqualTo(1);
		assertThat(published).isEmpty();

		secondTab.getRequest().getAsyncContext().complete();
		assertThat(registry.onlineGuestCount()).isZero();
		assertThat(published).hasSize(1).allMatch(PresenceChangedEvent.class::isInstance);
	}

	@Test
	void guestJoiningAnnouncesPresenceChangeOnlyForTheFirstStream() throws Exception {
		connect("guest-1");
		connect("guest-1");

		assertThat(published).hasSize(1).allMatch(PresenceChangedEvent.class::isInstance);
	}

	@Test
	void guestReconnectingAfterDisconnectIsCountedAgain() throws Exception {
		connect("guest-1").getRequest().getAsyncContext().complete();
		assertThat(registry.onlineGuestCount()).isZero();

		connect("guest-1");

		assertThat(registry.onlineGuestCount()).isEqualTo(1);
	}

	@Test
	void watchersAreNotCountedAsUsersOrGuests() {
		registry.registerWatcher();

		assertThat(registry.hasWatchers()).isTrue();
		assertThat(registry.onlineUserCount()).isZero();
		assertThat(registry.onlineGuestCount()).isZero();
		assertThat(published).isEmpty();
	}

	@Test
	void openingMoreStreamsThanTheCapKeepsTheGuestCountAtOne() throws Exception {
		for (int i = 0; i < 8; i++) connect("guest-1");

		assertThat(registry.onlineGuestCount()).isEqualTo(1);
	}

	private MvcResult connect(String guestId) throws Exception {
		return mockMvc.perform(get("/presence/guest-stream").requestAttr(GuestTrackingInterceptor.GUEST_ID_ATTRIBUTE, guestId))
				.andReturn();
	}
}
