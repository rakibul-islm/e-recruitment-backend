package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.ChangePasswordReqDto;
import com.bd.erecruitment.dto.req.RequestChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.VerifyChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.service.UserService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Off the "/user" prefix so self-service profile access doesn't require admin "user:*" authorities.
@RestApiController
@RequestMapping("/profile")
@Tag(name = "2.1 My Profile", description = "Self-service access to the logged-in user's own profile")
public class ProfileController {

	private final UserService<UserResDTO, UserReqDto> userService;

	public ProfileController(UserService<UserResDTO, UserReqDto> userService) {
		this.userService = userService;
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
