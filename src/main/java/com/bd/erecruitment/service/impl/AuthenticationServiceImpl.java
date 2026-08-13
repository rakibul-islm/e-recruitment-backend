package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.AuthenticationReqDTO;
import com.bd.erecruitment.dto.req.ForgotPasswordReqDto;
import com.bd.erecruitment.dto.req.GoogleAuthReqDTO;
import com.bd.erecruitment.dto.req.ResetPasswordReqDto;
import com.bd.erecruitment.dto.req.SetPasswordReqDto;
import com.bd.erecruitment.dto.req.VerifyOtpReqDto;
import com.bd.erecruitment.dto.res.AuthenticationResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.AuthenticationService;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.OtpService;
import com.bd.erecruitment.service.PasswordPolicyService;
import com.bd.erecruitment.util.JwtUtil;
import com.bd.erecruitment.util.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.security.core.GrantedAuthority;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AuthenticationServiceImpl extends AbstractBaseService<User> implements AuthenticationService<AuthenticationResDTO, AuthenticationReqDTO> {

	@Autowired private AuthenticationManager authenticationManager;
	@Autowired private JwtUtil jwtUtil;
	@Autowired private UserServiceImpl userService;
	@Autowired private PasswordPolicyService passwordPolicyService;
	@Autowired private MailService mailService;
	@Autowired private OtpService otpService;
	@Autowired private BCryptPasswordEncoder encoder;

	@Value("${google.client-id}")
	private String googleClientId;

	@Value("${app.otp.expiry-minutes:10}")
	private long otpExpiryMinutes;

	private final UserRepo userRepo;
	private final RestTemplate restTemplate = new RestTemplate();

	AuthenticationServiceImpl(UserRepo userRepo){
		super(userRepo);
		this.userRepo = userRepo;
	}

	@Override
	public Response<AuthenticationResDTO> find(Long id) { return null; }

	@Override
	public Response<AuthenticationResDTO> save(AuthenticationReqDTO reqDto) { return null; }

	@Override
	public Response<AuthenticationResDTO> update(AuthenticationReqDTO reqDto) { return null; }

	@Override
	public Response<AuthenticationResDTO> delete(Long id) { return null; }

	@Override
	public Response<AuthenticationResDTO> remove(Long id) { return null; }

	@Override
	@SuppressWarnings("unchecked")
	public Response<AuthenticationResDTO> loginWithGoogle(GoogleAuthReqDTO reqDto) {
		if (StringUtils.isBlank(reqDto.getIdToken())) returnUnauthorizedException("Google ID token is required");

		Map<String, Object> tokenInfo;
		try {
			ResponseEntity<Map> response = restTemplate.getForEntity(
					"https://oauth2.googleapis.com/tokeninfo?id_token=" + reqDto.getIdToken(), Map.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) returnUnauthorizedException("Invalid Google token");
			tokenInfo = response.getBody();
		} catch (Exception e) {
			returnUnauthorizedException("Invalid Google token");
			return null; // unreachable, satisfies compiler
		}

		if (!googleClientId.equals(tokenInfo.get("aud"))) returnUnauthorizedException("Invalid Google token audience");
		if (!"true".equals(String.valueOf(tokenInfo.get("email_verified")))) returnUnauthorizedException("Google email not verified");

		String googleId = (String) tokenInfo.get("sub");
		String email    = (String) tokenInfo.get("email");
		String fullName = (String) tokenInfo.get("name");

		User user = userRepo.findByGoogleId(googleId);
		if (user == null) {
			user = userRepo.findByEmail(email);
			if (user != null) {
				user.setGoogleId(googleId);
				userRepo.save(user);
			} else {
				user = new User();
				user.setGoogleId(googleId);
				user.setEmail(email);
				user.setFullName(fullName);
				user.setActive(true);
				user.setExpiryDate(getDefaultExpiryDate());
				userService.assignRegisteredUserRole(user);

				String pictureUrl = (String) tokenInfo.get("picture");
				if (StringUtils.isNotBlank(pictureUrl)) {
					try {
						user.setFileData(restTemplate.getForObject(pictureUrl, byte[].class));
					} catch (Exception e) {
						// Non-fatal: proceed even if the avatar fetch fails.
					}
				}

				user = createNormalUser(user);
			}
		}

		UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
		return getSuccessResponse("Login successful", buildAuthResponse(userDetails));
	}

	@Override
	public Response<AuthenticationResDTO> generateToken(AuthenticationReqDTO reqDto) {
		if (StringUtils.isBlank(reqDto.getEmail()) || StringUtils.isBlank(reqDto.getPassword())) returnErrorException("Email or password can't be empty");
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(reqDto.getEmail(), reqDto.getPassword()));
		} catch (BadCredentialsException e) {
			returnUnauthorizedException("Invalid email or password");
		} catch (DisabledException e) {
			returnUnauthorizedException("Your account isn't active yet. Please complete the verification/setup link sent to your email before signing in");
		}
		return getSuccessResponse("Token generated successfully", buildAuthResponse(userService.loadUserByUsername(reqDto.getEmail())));
	}

	@Override
	public Response<Object> forgotPassword(ForgotPasswordReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getEmail())) returnErrorException("Email is required");

		User user = userRepo.findByEmail(reqDto.getEmail());
		if (user != null) {
			String otp = otpService.generate(user);
			userRepo.save(user);

			try {
				mailService.sendOtpEmail(user.getEmail(), user.getFullName(), otp, otpExpiryMinutes);
			} catch (Exception e) {
				// Non-fatal: don't leak SMTP failures, or it'd reveal whether the email exists.
				log.error("Failed to send OTP email to {}", user.getEmail(), e);
			}
		}
		// Same message either way, to avoid leaking account existence.
		return getSuccessResponse("If an account exists for this email, an OTP has been sent");
	}

	@Override
	public Response<Object> verifyOtp(VerifyOtpReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getEmail()) || StringUtils.isBlank(reqDto.getOtp()))
			returnErrorException("Email and OTP are required");

		User user = userRepo.findByEmail(reqDto.getEmail());
		otpService.validate(user, reqDto.getOtp());
		return getSuccessResponse("OTP verified successfully");
	}

	@Override
	public Response<Object> resetPassword(ResetPasswordReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getEmail()) || StringUtils.isBlank(reqDto.getOtp()))
			returnErrorException("Email and OTP are required");
		if (StringUtils.isBlank(reqDto.getNewPassword()) || StringUtils.isBlank(reqDto.getConfirmPassword()))
			returnErrorException("New password and confirm password are required");
		if (!reqDto.getNewPassword().equals(reqDto.getConfirmPassword()))
			returnErrorException("New password and confirm password do not match");

		User user = userRepo.findByEmail(reqDto.getEmail());
		otpService.validate(user, reqDto.getOtp());

		if (StringUtils.isNotBlank(user.getPassword()) && encoder.matches(reqDto.getNewPassword(), user.getPassword()))
			returnErrorException("New password must be different from the current password");

		passwordPolicyService.validatePassword(reqDto.getNewPassword(), user.getEmail(), user.getFullName());

		user.setPassword(encoder.encode(reqDto.getNewPassword()));
		user.setOtpCode(null);
		user.setOtpExpiry(null);
		user.setOtpAttempts(0);
		userRepo.save(user);
		return getSuccessResponse("Password reset successfully");
	}

	@Override
	public Response<Object> setPassword(SetPasswordReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getToken())) returnErrorException("Token is required");
		if (StringUtils.isBlank(reqDto.getNewPassword()) || StringUtils.isBlank(reqDto.getConfirmPassword()))
			returnErrorException("New password and confirm password are required");
		if (!reqDto.getNewPassword().equals(reqDto.getConfirmPassword()))
			returnErrorException("New password and confirm password do not match");

		User user = userRepo.findByActivationToken(reqDto.getToken());
		if (user == null) returnErrorException("Invalid or expired link");

		if (user.getActivationTokenExpiry() == null || user.getActivationTokenExpiry().before(new Date())) {
			user.setActivationToken(null);
			user.setActivationTokenExpiry(null);
			userRepo.save(user);
			returnErrorException("Link has expired. Please ask an admin to recreate your account");
		}

		passwordPolicyService.validatePassword(reqDto.getNewPassword(), user.getEmail(), user.getFullName());

		user.setPassword(encoder.encode(reqDto.getNewPassword()));
		user.setActive(true);
		user.setActivationToken(null);
		user.setActivationTokenExpiry(null);
		userRepo.save(user);
		return getSuccessResponse("Password set successfully. You can now sign in");
	}

	private AuthenticationResDTO buildAuthResponse(UserDetails userDetails) {
		List<String> authorities = userDetails.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.toList());
		return AuthenticationResDTO.builder()
			.token(jwtUtil.generateToken(userDetails))
			.authorities(authorities)
			.build();
	}

}
