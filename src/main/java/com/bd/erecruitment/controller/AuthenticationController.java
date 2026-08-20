package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.AuthenticationReqDTO;
import com.bd.erecruitment.dto.req.ForgotPasswordReqDto;
import com.bd.erecruitment.dto.req.GoogleAuthReqDTO;
import com.bd.erecruitment.dto.req.ResetPasswordReqDto;
import com.bd.erecruitment.dto.req.SetPasswordReqDto;
import com.bd.erecruitment.dto.req.VerifyOtpReqDto;
import com.bd.erecruitment.dto.res.AuthenticationResDTO;
import com.bd.erecruitment.service.AuthenticationService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestApiController
@RequestMapping("/authenticate")
@Tag(name = "1.0 Authentication", description = "API")
public class AuthenticationController extends AbstractBaseController<AuthenticationResDTO, AuthenticationReqDTO> {

	private final AuthenticationService<AuthenticationResDTO, AuthenticationReqDTO> authService;

	public AuthenticationController(AuthenticationService<AuthenticationResDTO, AuthenticationReqDTO> authService) {
		super(authService);
		this.authService = authService;
	}

	@Operation(summary = "Generate access token")
	@PostMapping("/token")
	public ResponseEntity<Response<AuthenticationResDTO>> generateToken(@RequestBody AuthenticationReqDTO reqDto) {
		return respond(authService.generateToken(reqDto));
	}

	@Operation(summary = "Login with Google")
	@PostMapping("/google")
	public ResponseEntity<Response<AuthenticationResDTO>> loginWithGoogle(@RequestBody GoogleAuthReqDTO reqDto) {
		return respond(authService.loginWithGoogle(reqDto));
	}

	@Operation(summary = "Request a password reset OTP by email")
	@PostMapping("/forgot-password")
	public ResponseEntity<Response<Object>> forgotPassword(@RequestBody ForgotPasswordReqDto reqDto) {
		return respond(authService.forgotPassword(reqDto));
	}

	@Operation(summary = "Verify a password reset OTP")
	@PostMapping("/verify-otp")
	public ResponseEntity<Response<Object>> verifyOtp(@RequestBody VerifyOtpReqDto reqDto) {
		return respond(authService.verifyOtp(reqDto));
	}

	@Operation(summary = "Reset password using an OTP")
	@PostMapping("/reset-password")
	public ResponseEntity<Response<Object>> resetPassword(@RequestBody ResetPasswordReqDto reqDto) {
		return respond(authService.resetPassword(reqDto));
	}

	@Operation(summary = "Set the initial password for an admin-created account using the emailed activation link")
	@PostMapping("/set-password")
	public ResponseEntity<Response<Object>> setPassword(@RequestBody SetPasswordReqDto reqDto) {
		return respond(authService.setPassword(reqDto));
	}

	@Operation(summary = "Logout the current session")
	@PostMapping("/logout")
	public ResponseEntity<Response<Object>> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
		return respond(authService.logout(authorization));
	}

	@Hidden
	@PostMapping(value = "/oauth2/token", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public Map<String, String> generateTokenOAuth2(@RequestParam String username, @RequestParam String password) {
		AuthenticationReqDTO req = new AuthenticationReqDTO();
		req.setEmail(username);
		req.setPassword(password);
		Response<AuthenticationResDTO> response = authService.generateToken(req);
		if (!response.isSuccess()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, response.getMessage());
		}
		return Map.of("access_token", response.getObj().getToken(), "token_type", "bearer");
	}

	@Hidden @Override public ResponseEntity<Response<AuthenticationResDTO>> filter(@RequestParam Map<String, String> f, Pageable p, @RequestParam(required = false) Boolean ip) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuthenticationResDTO>> save(AuthenticationReqDTO e) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuthenticationResDTO>> update(AuthenticationReqDTO e) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuthenticationResDTO>> find(Long id) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuthenticationResDTO>> delete(Long id) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuthenticationResDTO>> remove(Long id) { return ResponseEntity.status(501).build(); }
}
