package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.RoleReqDto;
import com.bd.erecruitment.dto.res.RoleResDTO;
import com.bd.erecruitment.entity.Role;
import com.bd.erecruitment.repository.PermissionRepo;
import com.bd.erecruitment.repository.RoleRepo;
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
public class RoleServiceImpl extends AbstractBaseService<Role> implements BaseService<RoleResDTO, RoleReqDto> {

	private final PermissionRepo permissionRepo;

	RoleServiceImpl(RoleRepo roleRepo, PermissionRepo permissionRepo) {
		super(roleRepo);
		this.permissionRepo = permissionRepo;
	}

	@Transactional
	@Override
	public Response<RoleResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Found", new RoleResDTO(findByIdOrThrow(id, "Role not found")));
	}

	@Transactional
	@Override
	public Response<RoleResDTO> save(RoleReqDto reqDto) {
		validateForm(reqDto);
		Role role = reqDto.getBean();
		resolvePermissions(role, reqDto);
		return getCreatedResponse("Saved successfully", new RoleResDTO(createEntity(role)));
	}

	@Transactional
	@Override
	public Response<RoleResDTO> update(RoleReqDto reqDto) {
		if (reqDto.getId() == null) returnErrorException("Id required");
		validateForm(reqDto);
		Role existing = findByIdOrThrow(reqDto.getId(), "Role not found");
		existing.setName(reqDto.getName()).setCode(reqDto.getCode()).setDescription(reqDto.getDescription());
		resolvePermissions(existing, reqDto);
		return getSuccessResponse("Updated successfully", new RoleResDTO(updateEntity(existing)));
	}

	@Transactional
	@Override
	public Response<RoleResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "Role not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<RoleResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Role not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<RoleResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, RoleResDTO.class);
	}

	private void validateForm(RoleReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Name required");
		if (StringUtils.isBlank(reqDto.getCode())) returnErrorException("Code required");
	}

	private void resolvePermissions(Role role, RoleReqDto reqDto) {
		if (reqDto.getPermissionIds() != null && !reqDto.getPermissionIds().isEmpty())
			role.setPermissions(new HashSet<>(permissionRepo.findAllByIdInAndDeleted(new ArrayList<>(reqDto.getPermissionIds()), false)));
		else
			role.setPermissions(new HashSet<>());
	}
}
