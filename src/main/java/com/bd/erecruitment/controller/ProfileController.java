package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.service.UserService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Deliberately kept off the "/user" resource prefix: PermissionInterceptor derives the
// required authority from the first path segment, so nesting this under /user would make
// self-service profile access require the admin-only "user:read"/"user:write" authorities.
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
}
