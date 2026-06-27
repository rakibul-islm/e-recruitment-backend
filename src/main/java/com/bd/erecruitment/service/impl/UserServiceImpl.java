package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.UserSignupReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.repository.UserGroupRepo;
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

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;

@Service
public class UserServiceImpl extends AbstractBaseService<User> implements UserDetailsService, UserService<UserResDTO, UserReqDto> {

	private final UserRepo userRepo;
	private final BCryptPasswordEncoder encoder;
	private final RoleRepo roleRepo;
	private final UserGroupRepo userGroupRepo;

	public UserServiceImpl(UserRepo userRepo, BCryptPasswordEncoder encoder, RoleRepo roleRepo, UserGroupRepo userGroupRepo) {
		super(userRepo);
		this.userRepo = userRepo;
		this.encoder = encoder;
		this.roleRepo = roleRepo;
		this.userGroupRepo = userGroupRepo;
		// Prevent ModelMapper from triggering lazy load of roles in filter list response
		modelMapper.typeMap(User.class, UserResDTO.class)
			.addMappings(m -> m.skip(UserResDTO::setRoles));
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

	@Transactional
	@Override
	public Response<UserResDTO> save(UserReqDto reqDto) {
		validateForSave(reqDto.getUsername(), reqDto.getEmail(), reqDto.getPassword());
		User user = reqDto.getBean();
		user.setPassword(encoder.encode(user.getPassword()));
		if (reqDto.getExpiryDate() == null) user.setExpiryDate(getDefaultExpiryDate());
		resolveRolesAndGroups(user, reqDto);
		return getCreatedResponse("User saved successfully", new UserResDTO(createEntity(user)));
	}

	@Transactional
	@Override
	public Response<UserResDTO> saveNormalUser(UserSignupReqDto reqDto) {
		validateForSave(reqDto.getUsername(), reqDto.getEmail(), reqDto.getPassword());
		User user = reqDto.getBean();
		user.setPassword(encoder.encode(reqDto.getPassword()))
			.setExpiryDate(getDefaultExpiryDate())
			.setActive(true);
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
		resolveRolesAndGroups(exUser, reqDto);
		return getSuccessResponse("User updated successfully", new UserResDTO(updateEntity(exUser)));
	}

	@Transactional
	@Override
	public Response<UserResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "User not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<UserResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "User not found"));
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
		User byUsername = userRepo.findByUsername(reqDto.getUsername());
		if (byUsername != null && !byUsername.getId().equals(reqDto.getId())) returnErrorException("Username already exists");
		User byEmail = userRepo.findByEmail(reqDto.getEmail());
		if (byEmail != null && !byEmail.getId().equals(reqDto.getId())) returnErrorException("Email address already exists");
	}

	private void resolveRolesAndGroups(User user, UserReqDto reqDto) {
		if (reqDto.getRoleIds() != null && !reqDto.getRoleIds().isEmpty())
			user.setRoles(new HashSet<>(roleRepo.findAllByIdInAndDeleted(new ArrayList<>(reqDto.getRoleIds()), false)));
		if (reqDto.getUserGroupIds() != null && !reqDto.getUserGroupIds().isEmpty())
			user.setUserGroups(new HashSet<>(userGroupRepo.findAllByIdInAndDeleted(new ArrayList<>(reqDto.getUserGroupIds()), false)));
	}
}
