package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.UserSignupReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.enums.UserRole;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.UserService;
import com.bd.erecruitment.util.ImageUtils;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

@Service
public class UserServiceImpl extends AbstractBaseService<User> implements UserDetailsService, UserService<UserResDTO, UserReqDto> {

	private final UserRepo userRepo;
	private final BCryptPasswordEncoder encoder;

	public UserServiceImpl(UserRepo userRepo, BCryptPasswordEncoder encoder) {
		super(userRepo);
		this.userRepo = userRepo;
		this.encoder = encoder;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (StringUtils.isBlank(username)) return null;
		User user = userRepo.findByUsername(username);
		if (user == null) user = userRepo.findByEmail(username);
		if (user == null) throw new UsernameNotFoundException("No user found");
		return new MyUserDetail(user);
	}

	@Override
	public Response<UserResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		checkSelfOrAdminAccess(id);
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

	@Transactional
	@Override
	public Response<UserResDTO> save(UserReqDto reqDto) {
		if (isRole(UserRole.ROLE_CANDIDATE_USER)) returnForbiddenException("Unauthorized access");
		validateForSave(reqDto.getUsername(), reqDto.getEmail(), reqDto.getPassword());

		User user = reqDto.getBean();
		user.setPassword(encoder.encode(user.getPassword()));
		if (reqDto.getExpiryDate() == null) user.setExpiryDate(getDefaultExpiryDate());
		return getCreatedResponse("User saved successfully", new UserResDTO(createEntity(user)));
	}

	@Transactional
	@Override
	public Response<UserResDTO> saveNormalUser(UserSignupReqDto reqDto) {
		validateForSave(reqDto.getUsername(), reqDto.getEmail(), reqDto.getPassword());

		User user = reqDto.getBean();
		user.setPassword(encoder.encode(reqDto.getPassword()))
			.setExpiryDate(getDefaultExpiryDate())
			.setActive(true)
			.setCandidateUser(true)
			.setRecruiterUser(false)
			.setSuperAdmin(false)
			.setSystemAdmin(false);
		return getCreatedResponse("User saved successfully", new UserResDTO(createNormalUser(user)));
	}

	@Transactional
	@Override
	public Response<UserResDTO> update(UserReqDto reqDto) {
		validateForUpdate(reqDto);
		User exUser = findByIdOrThrow(reqDto.getId(), "User not found");
		reqDto.setPassword(StringUtils.isBlank(reqDto.getPassword()) ? exUser.getPassword() : encoder.encode(reqDto.getPassword()));
		modelMapper.map(reqDto, exUser);
		exUser.setFileData(StringUtils.isBlank(reqDto.getImageBase64()) ? exUser.getFileData() : Base64.getDecoder().decode(reqDto.getImageBase64()));
		return getSuccessResponse("User updated successfully", new UserResDTO(updateEntity(exUser)));
	}

	@Transactional
	@Override
	public Response<UserResDTO> delete(Long id) {
		deleteEntity(resolveUserForOperation(id, "delete"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<UserResDTO> remove(Long id) {
		removeEntity(resolveUserForOperation(id, "remove"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<UserResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, UserResDTO.class);
	}

	private void validateForSave(String username, String email, String password) {
		if (StringUtils.isBlank(password)) returnErrorException("Password required");
		if (userRepo.findByUsername(username) != null) returnErrorException("Username already exists");
		if (userRepo.findByEmail(email) != null) returnErrorException("Email address already exists");
	}

	private void validateForUpdate(UserReqDto reqDto) {
		if (reqDto.getId() == null) returnErrorException("User Id required");
		checkSelfOrAdminAccess(reqDto.getId());
		if (isRole(UserRole.ROLE_CANDIDATE_USER) && (reqDto.isSystemAdmin() || reqDto.isRecruiterUser() || reqDto.isSuperAdmin()))
			returnForbiddenException("Unauthorized access");
		User byUsername = userRepo.findByUsername(reqDto.getUsername());
		if (byUsername != null && !byUsername.getId().equals(reqDto.getId())) returnErrorException("Username already exists");
		User byEmail = userRepo.findByEmail(reqDto.getEmail());
		if (byEmail != null && !byEmail.getId().equals(reqDto.getId())) returnErrorException("Email address already exists");
	}

	private void checkSelfOrAdminAccess(Long id) {
		if (isRole(UserRole.ROLE_CANDIDATE_USER) && !getLoggedInUserDetails().getId().equals(id))
			returnForbiddenException("Unauthorized access");
	}

	private User resolveUserForOperation(Long id, String action) {
		checkSelfOrAdminAccess(id);
		User user = findByIdOrThrow(id, "User not found");
		if (user.isSuperAdmin()) returnForbiddenException("Can't " + action + " super admin user");
		user.setLocked(true);
		return user;
	}
}
