package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.ChangePasswordReqDto;
import com.bd.erecruitment.dto.req.RequestChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.req.ResendSignupOtpReqDto;
import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.UserSignupReqDto;
import com.bd.erecruitment.dto.req.VerifyChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.req.VerifySignupOtpReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.entity.Role;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.OtpService;
import com.bd.erecruitment.service.PasswordPolicyService;
import com.bd.erecruitment.service.UserService;
import com.bd.erecruitment.util.ImageUtils;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.PropertyMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl extends AbstractBaseService<User> implements UserDetailsService, UserService<UserResDTO, UserReqDto> {

	private final UserRepo userRepo;
	private final BCryptPasswordEncoder encoder;
	private final RoleRepo roleRepo;
	private final PasswordPolicyService passwordPolicyService;
	private final MailService mailService;
	private final OtpService otpService;

	@Value("${app.otp.expiry-minutes:10}")
	private long otpExpiryMinutes;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	@Value("${app.account-setup.expiry-hours:48}")
	private long activationExpiryHours;

	public UserServiceImpl(UserRepo userRepo, BCryptPasswordEncoder encoder, RoleRepo roleRepo,
			PasswordPolicyService passwordPolicyService, MailService mailService, OtpService otpService) {
		super(userRepo);
		this.userRepo = userRepo;
		this.encoder = encoder;
		this.roleRepo = roleRepo;
		this.passwordPolicyService = passwordPolicyService;
		this.mailService = mailService;
		this.otpService = otpService;
		// PropertyMap.skip() avoids ModelMapper triggering a lazy load of roles here.
		modelMapper.addMappings(new PropertyMap<User, UserResDTO>() {
			@Override
			protected void configure() {
				skip(destination.getRoles());
			}
		});
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (StringUtils.isBlank(username)) return null;
		User user = userRepo.findByLoginWithPermissions(username)
			.orElseThrow(() -> new UsernameNotFoundException("No user found"));
		return new MyUserDetail(user);
	}

	@Transactional
	@Override
	public Response<UserResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("User found", new UserResDTO(findByIdOrThrow(id, "User not found")));
	}

	@Override
	public Response<UserProfileResDTO> userProfile() {
		User user = findByIdOrThrow(getLoggedInUserDetails().getId(), "User not found");
		user.setImageBase64(user.getFileData() == null
			? ImageUtils.DEFAULT_AVATAR_BASE64
			: Base64.getEncoder().encodeToString(user.getFileData()));
		return getSuccessResponse("User found", new UserProfileResDTO(user));
	}

	// Always targets the logged-in user's own record — id/roles/userGroup are never taken from the request.
	@Transactional
	@Override
	public Response<UserResDTO> updateProfile(UserReqDto reqDto) {
		User exUser = findByIdOrThrow(getLoggedInUserDetails().getId(), "User not found");
		User byEmail = userRepo.findByEmail(reqDto.getEmail());
		if (byEmail != null && !byEmail.getId().equals(exUser.getId())) returnErrorException("Email address already exists");

		reqDto.setId(exUser.getId());
		if (StringUtils.isNotBlank(reqDto.getPassword()))
			passwordPolicyService.validatePassword(reqDto.getPassword(), reqDto.getEmail(), reqDto.getFullName());
		reqDto.setPassword(StringUtils.isBlank(reqDto.getPassword()) ? exUser.getPassword() : encoder.encode(reqDto.getPassword()));
		// The profile form doesn't send these, so an unguarded map() would wipe them with UserReqDto's defaults.
		reqDto.setActive(exUser.isActive());
		reqDto.setLocked(exUser.isLocked());
		reqDto.setExpiryDate(exUser.getExpiryDate());
		modelMapper.map(reqDto, exUser);
		exUser.setFileData(StringUtils.isBlank(reqDto.getImageBase64()) ? exUser.getFileData() : Base64.getDecoder().decode(reqDto.getImageBase64()));
		return getSuccessResponse("Profile updated successfully", new UserResDTO(updateEntity(exUser)));
	}

	@Transactional
	@Override
	public Response<Object> requestChangePasswordOtp(RequestChangePasswordOtpReqDto reqDto) {
		User user = findByIdOrThrow(getLoggedInUserDetails().getId(), "User not found");
		if (StringUtils.isBlank(reqDto.getOldPassword())) returnErrorException("Current password is required");
		if (!encoder.matches(reqDto.getOldPassword(), user.getPassword()))
			returnErrorException("Current password is incorrect");

		String otp = otpService.generate(user);
		userRepo.save(user);
		try {
			mailService.sendChangePasswordOtpEmail(user.getEmail(), user.getFullName(), otp, otpExpiryMinutes);
		} catch (Exception e) {
			log.error("Failed to send change-password OTP email to {}", user.getEmail(), e);
		}
		return getSuccessResponse("An OTP has been sent to your email");
	}

	@Override
	public Response<Object> verifyChangePasswordOtp(VerifyChangePasswordOtpReqDto reqDto) {
		User user = findByIdOrThrow(getLoggedInUserDetails().getId(), "User not found");
		if (StringUtils.isBlank(reqDto.getOtp())) returnErrorException("OTP is required");
		otpService.validate(user, reqDto.getOtp());
		return getSuccessResponse("OTP verified successfully");
	}

	@Transactional
	@Override
	public Response<Object> changePassword(ChangePasswordReqDto reqDto) {
		User user = findByIdOrThrow(getLoggedInUserDetails().getId(), "User not found");
		if (StringUtils.isBlank(reqDto.getOldPassword()) || StringUtils.isBlank(reqDto.getOtp()) || StringUtils.isBlank(reqDto.getNewPassword()))
			returnErrorException("Old password, OTP and new password are required");
		if (!encoder.matches(reqDto.getOldPassword(), user.getPassword()))
			returnErrorException("Old password is incorrect");
		otpService.validate(user, reqDto.getOtp());
		if (!reqDto.getNewPassword().equals(reqDto.getConfirmPassword()))
			returnErrorException("New password and confirm password do not match");
		if (encoder.matches(reqDto.getNewPassword(), user.getPassword()))
			returnErrorException("New password must be different from the current password");
		passwordPolicyService.validatePassword(reqDto.getNewPassword(), user.getEmail(), user.getFullName());
		user.setPassword(encoder.encode(reqDto.getNewPassword()));
		user.setOtpCode(null);
		user.setOtpExpiry(null);
		user.setOtpAttempts(0);
		updateEntity(user);
		return getSuccessResponse("Password changed successfully");
	}

	// Admin-created accounts start inactive with an emailed activation link; the admin never sets/knows the password.
	@Transactional
	@Override
	public Response<UserResDTO> save(UserReqDto reqDto) {
		validateForSave(reqDto.getEmail());
		User user = reqDto.getBean();
		user.setPassword(null);
		user.setActive(false);
		user.setActivationToken(UUID.randomUUID().toString());
		user.setActivationTokenExpiry(new Date(System.currentTimeMillis() + activationExpiryHours * 3600_000L));
		if (reqDto.getExpiryDate() == null) user.setExpiryDate(getDefaultExpiryDate());
		resolveRolesAndGroups(user, reqDto);
		User saved = createEntity(user);
		sendAccountSetupEmail(saved);
		return getCreatedResponse("User saved successfully", new UserResDTO(saved));
	}

	private void sendAccountSetupEmail(User user) {
		String link = frontendBaseUrl + "/set-password?token=" + user.getActivationToken();
		try {
			mailService.sendAccountSetupEmail(user.getEmail(), user.getFullName(), link, activationExpiryHours);
		} catch (Exception e) {
			log.error("Failed to send account setup email to {}", user.getEmail(), e);
		}
	}

	@Transactional
	@Override
	public Response<UserResDTO> saveNormalUser(UserSignupReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getPassword())) returnErrorException("Password required");
		passwordPolicyService.validatePassword(reqDto.getPassword(), reqDto.getEmail(), reqDto.getFullName());

		User existing = userRepo.findByEmail(reqDto.getEmail());
		User user;
		if (existing != null) {
			if (existing.isActive()) returnErrorException("Email address already exists");
			// Pending, unverified signup — refresh details and resend a fresh OTP instead of erroring.
			existing.setFullName(reqDto.getFullName())
					.setMobile(reqDto.getMobile())
					.setExpiryDate(getDefaultExpiryDate())
					.setPassword(encoder.encode(reqDto.getPassword()));
			assignRegisteredUserRole(existing);
			user = userRepo.save(existing);
		} else {
			User newUser = reqDto.getBean();
			newUser.setPassword(encoder.encode(reqDto.getPassword()))
				.setExpiryDate(getDefaultExpiryDate())
				.setActive(false);
			assignRegisteredUserRole(newUser);
			user = createNormalUser(newUser);
		}

		sendSignupOtp(user);
		return getCreatedResponse("Account created. An OTP has been sent to verify it", new UserResDTO(user));
	}

	@Transactional
	@Override
	public Response<Object> verifySignupOtp(VerifySignupOtpReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getEmail()) || StringUtils.isBlank(reqDto.getOtp()))
			returnErrorException("Email and OTP are required");

		User user = userRepo.findByEmail(reqDto.getEmail());
		otpService.validate(user, reqDto.getOtp());

		user.setActive(true);
		user.setOtpCode(null);
		user.setOtpExpiry(null);
		user.setOtpAttempts(0);
		userRepo.save(user);
		return getSuccessResponse("Account verified successfully. You can now sign in");
	}

	@Transactional
	@Override
	public Response<Object> resendSignupOtp(ResendSignupOtpReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getEmail())) returnErrorException("Email is required");

		User user = userRepo.findByEmail(reqDto.getEmail());
		if (user != null && !user.isActive()) {
			sendSignupOtp(user);
		}
		// Same message either way, to avoid leaking account state.
		return getSuccessResponse("If a pending signup exists for this email, a new OTP has been sent");
	}

	private void sendSignupOtp(User user) {
		String otp = otpService.generate(user);
		userRepo.save(user);

		try {
			mailService.sendSignupOtpEmail(user.getEmail(), user.getFullName(), otp, otpExpiryMinutes);
		} catch (Exception e) {
			log.error("Failed to send signup OTP email to {}", user.getEmail(), e);
		}
	}

	public void assignRegisteredUserRole(User user) {
		Role registeredUserRole = roleRepo.findByCode("REGISTERED_USER");
		if (registeredUserRole != null) {
			user.getRoles().add(registeredUserRole);
		}
	}

	@Transactional
	@Override
	public Response<UserResDTO> update(UserReqDto reqDto) {
		validateForUpdate(reqDto);
		User exUser = findByIdOrThrow(reqDto.getId(), "User not found");
		// Password can only be set via the OTP-verified self-service change-password flow.
		reqDto.setPassword(exUser.getPassword());
		modelMapper.map(reqDto, exUser);
		exUser.setFileData(StringUtils.isBlank(reqDto.getImageBase64()) ? exUser.getFileData() : Base64.getDecoder().decode(reqDto.getImageBase64()));
		resolveRolesAndGroups(exUser, reqDto);
		return getSuccessResponse("User updated successfully", new UserResDTO(updateEntity(exUser)));
	}

	@Transactional
	@Override
	public Response<UserResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "User not found"));
		return getSuccessResponse("Deleted successfully");
	}

	// Soft delete keeps the row, so email/googleId (both unique) must be freed here or the
	@Transactional
	@Override
	public Response<UserResDTO> remove(Long id) {
		User user = findByIdOrThrow(id, "User not found");
		String suffix = "removed+" + System.currentTimeMillis() + "+";
		user.setEmail(suffix + user.getEmail());
		if (StringUtils.isNotBlank(user.getGoogleId())) user.setGoogleId(suffix + user.getGoogleId());
		removeEntity(user);
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<UserResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, UserResDTO.class);
	}

	private void validateForSave(String email) {
		if (userRepo.findByEmail(email) != null) returnErrorException("Email address already exists");
	}

	private void validateForUpdate(UserReqDto reqDto) {
		if (reqDto.getId() == null) returnErrorException("User Id required");
		User byEmail = userRepo.findByEmail(reqDto.getEmail());
		if (byEmail != null && !byEmail.getId().equals(reqDto.getId())) returnErrorException("Email address already exists");
	}

	private void resolveRolesAndGroups(User user, UserReqDto reqDto) {
		if (reqDto.getRoleIds() != null && !reqDto.getRoleIds().isEmpty())
			user.setRoles(new HashSet<>(roleRepo.findAllByIdInAndDeleted(new ArrayList<>(reqDto.getRoleIds()), false)));
		user.setUserGroupId(reqDto.getUserGroupId());
	}
}
