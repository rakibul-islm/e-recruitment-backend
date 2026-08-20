package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.UserSessionReqDto;
import com.bd.erecruitment.dto.res.SessionSummaryResDTO;
import com.bd.erecruitment.dto.res.UserSessionResDTO;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/session")
@Tag(name = "Session Management")
public class UserSessionController extends AbstractBaseController<UserSessionResDTO, UserSessionReqDto> {

	private final UserSessionService sessionService;

	UserSessionController(UserSessionService sessionService) {
		super(sessionService);
		this.sessionService = sessionService;
	}

	@Operation(summary = "Active session and guest count summary")
	@GetMapping("/summary")
	public ResponseEntity<Response<SessionSummaryResDTO>> summary() {
		return respond(sessionService.getSummary());
	}

	@Operation(summary = "List sessions for a specific user")
	@GetMapping("/user/{userId}")
	public ResponseEntity<Response<UserSessionResDTO>> byUser(@PathVariable Long userId) {
		return respond(sessionService.findByUser(userId));
	}

	@Operation(summary = "Force logout all active sessions for a user")
	@DeleteMapping("/user/{userId}")
	public ResponseEntity<Response<Object>> forceLogoutUser(@PathVariable Long userId) {
		return respond(sessionService.forceLogoutUser(userId));
	}

	@Operation(summary = "Force logout every active session in the system")
	@DeleteMapping("/all")
	public ResponseEntity<Response<Object>> forceLogoutAll() {
		return respond(sessionService.forceLogoutAll());
	}

	@Hidden @Override public ResponseEntity<Response<UserSessionResDTO>> save(UserSessionReqDto e) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<UserSessionResDTO>> update(UserSessionReqDto e) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<UserSessionResDTO>> delete(Long id) { return ResponseEntity.status(501).build(); }
}
