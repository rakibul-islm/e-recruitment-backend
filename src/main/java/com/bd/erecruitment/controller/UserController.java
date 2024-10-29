package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.UserSignupReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.service.UserService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestApiController
@RequestMapping("/e-recruitment/user")
@Tag(name = "2.0 User", description = "API")
public class UserController extends AbstractBaseController<User, UserResDTO, UserReqDto>{
	
	private UserService<UserResDTO, UserReqDto> userService;

	public UserController(UserService<UserResDTO, UserReqDto> userService) {
		super(userService);
		this.userService = userService;
	}
	
	@Operation(summary = "signup")
	@PostMapping("/signup")
	public Response<UserResDTO> signup(@RequestBody UserSignupReqDto reqDto) throws Exception {
		return userService.saveNormalUser(reqDto);
	}

	@Operation(summary = "User Profile")
	@GetMapping("/user-profile")
	public Response<UserProfileResDTO> userProfile() throws Exception {
		return userService.userProfile();
	}
}
