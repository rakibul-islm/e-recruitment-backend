package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.AuthenticationReqDTO;
import com.bd.erecruitment.dto.req.GoogleAuthReqDTO;
import com.bd.erecruitment.dto.res.AuthenticationResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.AuthenticationService;
import com.bd.erecruitment.util.JwtUtil;
import com.bd.erecruitment.util.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuthenticationServiceImpl extends AbstractBaseService<User> implements AuthenticationService<AuthenticationResDTO, AuthenticationReqDTO> {

	@Autowired private AuthenticationManager authenticationManager;
	@Autowired private JwtUtil jwtUtil;
	@Autowired private UserServiceImpl userService;

	@Value("${google.client-id}")
	private String googleClientId;

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
				user.setUsername(email);
				user.setActive(true);
				user.setExpiryDate(getDefaultExpiryDate());
				user = createNormalUser(user);
			}
		}

		UserDetails userDetails = userService.loadUserByUsername(user.getUsername());
		return getSuccessResponse("Login successful", buildAuthResponse(userDetails));
	}

	@Override
	public Response<AuthenticationResDTO> generateToken(AuthenticationReqDTO reqDto) {
		if (StringUtils.isBlank(reqDto.getUsername()) || StringUtils.isBlank(reqDto.getPassword())) returnErrorException("Username or password can't be empty");
		try {
			authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(reqDto.getUsername(), reqDto.getPassword()));
		} catch (BadCredentialsException e) {
			returnUnauthorizedException("Invalid username or password");
		}
		return getSuccessResponse("Token generated successfully", buildAuthResponse(userService.loadUserByUsername(reqDto.getUsername())));
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
