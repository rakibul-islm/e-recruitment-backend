package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.UserGroupReqDto;
import com.bd.erecruitment.dto.res.UserGroupResDTO;
import com.bd.erecruitment.entity.UserGroup;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.repository.UserGroupRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;

@Service
public class UserGroupServiceImpl extends AbstractBaseService<UserGroup> implements BaseService<UserGroupResDTO, UserGroupReqDto> {

	private final UserGroupRepo userGroupRepo;
	private final RoleRepo roleRepo;

	UserGroupServiceImpl(UserGroupRepo userGroupRepo, RoleRepo roleRepo) {
		super(userGroupRepo);
		this.userGroupRepo = userGroupRepo;
		this.roleRepo = roleRepo;
	}

	@Transactional
	@Override
	public Response<UserGroupResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		UserGroup group = userGroupRepo.findByIdWithDetails(id)
			.orElseThrow(() -> new NotFoundException("User group not found"));
		return getSuccessResponse("Found", new UserGroupResDTO(group));
	}

	@Transactional
	@Override
	public Response<UserGroupResDTO> save(UserGroupReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Name required");
		UserGroup group = reqDto.getBean();
		resolveRoles(group, reqDto);
		return getCreatedResponse("Saved successfully", new UserGroupResDTO(createEntity(group)));
	}

	@Transactional
	@Override
	public Response<UserGroupResDTO> update(UserGroupReqDto reqDto) {
		if (reqDto.getId() == null) returnErrorException("Id required");
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Name required");
		UserGroup existing = findByIdOrThrow(reqDto.getId(), "User group not found");
		existing.setName(reqDto.getName()).setDescription(reqDto.getDescription());
		resolveRoles(existing, reqDto);
		return getSuccessResponse("Updated successfully", new UserGroupResDTO(updateEntity(existing)));
	}

	@Transactional
	@Override
	public Response<UserGroupResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "User group not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<UserGroupResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "User group not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<UserGroupResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, UserGroupResDTO.class);
	}

	private void resolveRoles(UserGroup group, UserGroupReqDto reqDto) {
		if (reqDto.getRoleIds() != null && !reqDto.getRoleIds().isEmpty())
			group.setRoles(new HashSet<>(roleRepo.findAllByIdInAndDeleted(new ArrayList<>(reqDto.getRoleIds()), false)));
		else
			group.setRoles(new HashSet<>());
	}
}
