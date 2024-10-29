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
import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl extends AbstractBaseService<User, UserProfileResDTO, UserReqDto> implements UserDetailsService, UserService<UserResDTO, UserReqDto> {

	private UserRepo userRepo;
	@Autowired
	private BCryptPasswordEncoder encoder;

	UserServiceImpl(UserRepo uRepo){
		super(uRepo);
		this.userRepo = uRepo;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (StringUtils.isBlank(username)) return null;

		User user = userRepo.findByUsername(username);
		if(user == null) user = userRepo.findByEmail(username);
		if (user == null) throw new UsernameNotFoundException("No User found !!");

		return new MyUserDetail(user);
	}

	@Override
	public Response<UserResDTO> find(Long id) throws ServiceException {
		if(getLoggedInUser().getRoles().equalsIgnoreCase(UserRole.ROLE_CANDIDATE_USER.name()) && !getLoggedInUserDetails().getId().equals(id)) return getErrorResponse("Unauthorized Access!");
		if(id == null) return getErrorResponse("Id required");

		Optional<User> o = userRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("User found", new UserResDTO(o.get())) : getErrorResponse("User not found");
	}
	
	@Override
	public Response<UserProfileResDTO> userProfile() throws ServiceException{
		Long id = getLoggedInUserDetails().getId();

		Optional<User> o = userRepo.findByIdAndDeleted(id, false);
		if(o.isPresent()) {
			if(o.get().getFileData() == null ) {
				o.get().setImageBase64("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");
			}else {
				o.get().setImageBase64(Base64.encodeBase64String(o.get().getFileData()));
			}
		}

		return o.isPresent() ? getSuccessResponse("User found", new UserProfileResDTO(o.get())) : getErrorResponse("User not found");
	}

	@Transactional
	@Override
	public Response<UserResDTO> save(UserReqDto reqDto) throws ServiceException{
		// validation
		if(getLoggedInUser().getRoles().equalsIgnoreCase(UserRole.ROLE_CANDIDATE_USER.name())) return getErrorResponse("Unauthorized Access!");
		if(userRepo.findByUsername(reqDto.getUsername()) != null) return getErrorResponse("Username already exist! Try with different one");
		if(userRepo.findByEmail(reqDto.getEmail()) != null) return getErrorResponse("Email address already exist! Try with different one");
		if(StringUtils.isBlank(reqDto.getPassword())) return getErrorResponse("Password required");

		User user = reqDto.getBean();
		user.setPassword(encoder.encode(user.getPassword()));

		if(reqDto.getExpiryDate() == null) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.YEAR, 50);
			user.setExpiryDate(cal.getTime());
		}

		user = createEntity(user);
		if(user == null) return getErrorResponse("Can't save user");

		return getSuccessResponse("User saved successfully", new UserResDTO(user));
	}
	
	@Transactional
	@Override
	public Response<UserResDTO> saveNormalUser(UserSignupReqDto reqDto) throws ServiceException{
		// validation
		if(userRepo.findByUsername(reqDto.getUsername()) != null) return getErrorResponse("Username already exist! Try with different one");
		if(userRepo.findByEmail(reqDto.getEmail()) != null) return getErrorResponse("Email address already exist! Try with different one");
		if(StringUtils.isBlank(reqDto.getPassword())) return getErrorResponse("Password required");

		User user = reqDto.getBean();
		user.setPassword(encoder.encode(user.getPassword()));
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 50);
		user.setExpiryDate(cal.getTime());
		user.setActive(true);
		user.setDeleted(false);
		user.setCandidateUser(true);
		user.setRecruiterUser(false);
		user.setSuperAdmin(false);
		user.setSystemAdmin(false);

		user = createNormalUser(user);
		if(user == null) return getErrorResponse("Can't save user");

		return getSuccessResponse("User saved successfully", new UserResDTO(user));
	}

	@Transactional
	@Override
	public Response<UserResDTO> update(UserReqDto reqDto) throws ServiceException {
		if(getLoggedInUser().getRoles().equalsIgnoreCase(UserRole.ROLE_CANDIDATE_USER.name()) && !getLoggedInUserDetails().getId().equals(reqDto.getId())) return getErrorResponse("Unauthorized Access!");
		if(getLoggedInUser().getRoles().equalsIgnoreCase(UserRole.ROLE_CANDIDATE_USER.name()) && (reqDto.isSystemAdmin() || reqDto.isRecruiterUser() || reqDto.isSuperAdmin())) return getErrorResponse("Unauthorized Access!");
		if(reqDto.getId() == null) return getErrorResponse("User Id required");
		User u1 = userRepo.findByUsername(reqDto.getUsername());
		if(u1 != null &&  !u1.getId().equals(reqDto.getId())) return getErrorResponse("Username already exist! Try with different one");
		User u2 = userRepo.findByEmail(reqDto.getEmail());
		if(u2 != null && !u2.getId().equals(reqDto.getId())) return getErrorResponse("Email address already exist! Try with different one");

		Optional<User> exOp = userRepo.findById(reqDto.getId());
		if(!exOp.isPresent()) return getErrorResponse("User not found in this system");

		User exUser = exOp.get();
		if(StringUtils.isBlank(reqDto.getPassword())) {
			reqDto.setPassword(exUser.getPassword());
		} else {
			reqDto.setPassword(encoder.encode(reqDto.getPassword()));
		}
		
		if(reqDto.getImageBase64() == null || StringUtils.isBlank(reqDto.getImageBase64())) {
			exUser.setFileData(exUser.getFileData());
		}else {
			exUser.setFileData(Base64.decodeBase64(reqDto.getImageBase64()));
		}
		
		BeanUtils.copyProperties(reqDto, exUser);

		exUser = updateEntity(exUser);
		if(exUser == null) return getErrorResponse("Can't update user info");

		return getSuccessResponse("User updated successfully", new UserResDTO(exUser));
	}

	@Override
	public Response<UserResDTO> getAll(Pageable pageable, Boolean isPageable) throws ServiceException {
		if(getLoggedInUser().getRoles().equalsIgnoreCase(UserRole.ROLE_CANDIDATE_USER.name())) return getErrorResponse("Unauthorized Access!");

		if(Boolean.TRUE.equals(isPageable)) {
			Page<User> page = userRepo.findAllByDeleted(false, pageable);
			if(!page.hasContent()) return getErrorResponse("User not found in this system");

			return getSuccessResponse(
					"Found Users",
					page.map(data -> new ModelMapper().map(data, UserResDTO.class))
			);
		}
		List<User> list = userRepo.findAllByDeleted(false);
		if(list == null || list.isEmpty()) return getErrorResponse("User not found in this system");

		return getSuccessResponse(
				"Found Users",
				list.stream().map(data -> new ModelMapper().map(data, UserResDTO.class))
						.collect(Collectors.toList())
		);
	}

	@Transactional
	@Override
	public Response<UserResDTO> delete(UserReqDto reqDto) throws ServiceException {
		if(getLoggedInUser().getRoles().equalsIgnoreCase(UserRole.ROLE_CANDIDATE_USER.name()) && !getLoggedInUserDetails().getId().equals(reqDto.getId())) return getErrorResponse("Unauthorized Access!");
		User z = reqDto.getBean();

		User exist = null;
		Optional<User> o = userRepo.findByIdAndDeleted(z.getId(), false);
		if(!o.isPresent()) return getErrorResponse("User not found in system");
		exist = o.get();
		if(exist.isSuperAdmin()) return getErrorResponse("Can't delete super admin user");
		exist.setLocked(true);
		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Transactional
	@Override
	public Response<UserResDTO> remove(Long id) throws ServiceException {
		if(getLoggedInUser().getRoles().equalsIgnoreCase(UserRole.ROLE_CANDIDATE_USER.name()) && !getLoggedInUserDetails().getId().equals(id)) return getErrorResponse("Unauthorized Access!");
		User exist = null;
		Optional<User> o = userRepo.findByIdAndDeleted(id, false);
		if(!o.isPresent()) return getErrorResponse("User not found in system");
		exist = o.get();
		if(exist.isSuperAdmin()) return getErrorResponse("Can't remove super admin user");
		exist.setLocked(true);
		
		try {
			removeEntityById(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}
	
}
