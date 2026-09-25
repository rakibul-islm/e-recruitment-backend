package com.bd.erecruitment.notification;

import com.bd.erecruitment.dto.res.SessionSummaryResDTO;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.util.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PresenceBroadcasterTest {

	private SseEmitterRegistry registry;
	private UserSessionService sessionService;
	private PresenceBroadcaster broadcaster;

	@BeforeEach
	void setUp() {
		registry = new SseEmitterRegistry(event -> { });
		sessionService = mock(UserSessionService.class);
		Response<SessionSummaryResDTO> response = new Response<>();
		response.setObj(new SessionSummaryResDTO(1, 1, 1));
		when(sessionService.getSummary()).thenReturn(response);
		broadcaster = new PresenceBroadcaster(registry, sessionService);
		registry.registerWatcher();
	}

	@Test
	void aBurstOfChangesIsBroadcastOnceOnTheNextFlush() {
		for (int i = 0; i < 500; i++) broadcaster.onPresenceChanged(new PresenceChangedEvent());

		broadcaster.flushIfChanged();
		broadcaster.flushIfChanged();

		verify(sessionService, times(1)).getSummary();
	}

	@Test
	void nothingIsQueriedWhenNothingChanged() {
		broadcaster.flushIfChanged();

		verify(sessionService, never()).getSummary();
	}

	@Test
	void nothingIsQueriedWhenNoAdminIsWatching() {
		PresenceBroadcaster unwatched = new PresenceBroadcaster(new SseEmitterRegistry(event -> { }), sessionService);
		unwatched.onPresenceChanged(new PresenceChangedEvent());

		unwatched.flushIfChanged();

		verify(sessionService, never()).getSummary();
	}
}
