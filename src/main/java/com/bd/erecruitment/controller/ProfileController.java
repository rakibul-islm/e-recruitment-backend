package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.ChangePasswordReqDto;
import com.bd.erecruitment.dto.req.RequestChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.VerifyChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.service.UserService;
import com.bd.erecruitment.service.UserSessionService;
import com.bd.erecruitment.util.JwtUtil;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/profile")
@Tag(name = "2.1 My Profile", description = "Self-service access to the logged-in user's own profile")
public class ProfileController {

	private final UserService<UserResDTO, UserReqDto> userService;
	private final UserSessionService userSessionService;
	private final JwtUtil jwtUtil;

	public ProfileController(UserService<UserResDTO, UserReqDto> userService, UserSessionService userSessionService, JwtUtil jwtUtil) {
		this.userService = userService;
		this.userSessionService = userSessionService;
		this.jwtUtil = jwtUtil;
	}

	@Operation(summary = "Get my profile")
	@GetMapping
	public Response<UserProfileResDTO> getProfile() {
		return userService.userProfile();
	}

	@Operation(summary = "Update my profile")
	@PutMapping
	public Response<UserResDTO> updateProfile(@RequestBody UserReqDto reqDto) {
		return userService.updateProfile(reqDto);
	}

	@Operation(summary = "Record the exact city and country (X-Client-City / X-Client-Country headers) on my current login session")
	@PutMapping("/session-location")
	public Response<Object> updateSessionLocation(@RequestHeader(value = "Authorization", required = false) String authorization) {
		String jti = null;
		if (authorization != null && authorization.startsWith("Bearer ")) {
			try {
				jti = jwtUtil.extractJti(authorization.substring(7));
			} catch (Exception ignored) {}
		}
		return userSessionService.updateCurrentSessionLocation(jti);
	}

	@Operation(summary = "Request an OTP to confirm a password change")
	@PostMapping("/change-password/request-otp")
	public Response<Object> requestChangePasswordOtp(@RequestBody RequestChangePasswordOtpReqDto reqDto) {
		return userService.requestChangePasswordOtp(reqDto);
	}

	@Operation(summary = "Verify the OTP for a password change")
	@PostMapping("/change-password/verify-otp")
	public Response<Object> verifyChangePasswordOtp(@RequestBody VerifyChangePasswordOtpReqDto reqDto) {
		return userService.verifyChangePasswordOtp(reqDto);
	}

	@Operation(summary = "Change my password")
	@PutMapping("/change-password")
	public Response<Object> changePassword(@RequestBody ChangePasswordReqDto reqDto) {
		return userService.changePassword(reqDto);
	}
}
