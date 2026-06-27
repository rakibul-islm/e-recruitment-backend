package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.PermissionReqDto;
import com.bd.erecruitment.dto.res.PermissionResDTO;
import com.bd.erecruitment.entity.Permission;
import com.bd.erecruitment.repository.PermissionRepo;
import com.bd.erecruitment.security.PermissionInterceptor;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PermissionServiceImpl extends AbstractBaseService<Permission> implements BaseService<PermissionResDTO, PermissionReqDto> {

	private final PermissionInterceptor permissionInterceptor;

	PermissionServiceImpl(PermissionRepo permissionRepo, PermissionInterceptor permissionInterceptor) {
		super(permissionRepo);
		this.permissionInterceptor = permissionInterceptor;
	}

	@Override
	public Response<PermissionResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Found", new PermissionResDTO(findByIdOrThrow(id, "Permission not found")));
	}

	@Transactional
	@Override
	public Response<PermissionResDTO> save(PermissionReqDto reqDto) {
		validateForm(reqDto);
		Response<PermissionResDTO> response = getCreatedResponse("Saved successfully", new PermissionResDTO(createEntity(reqDto.getBean())));
		permissionInterceptor.invalidateCache();
		return response;
	}

	@Transactional
	@Override
	public Response<PermissionResDTO> update(PermissionReqDto reqDto) {
		if (reqDto.getId() == null) returnErrorException("Id required");
		validateForm(reqDto);
		Permission existing = findByIdOrThrow(reqDto.getId(), "Permission not found");
		modelMapper.map(reqDto, existing);
		Response<PermissionResDTO> response = getSuccessResponse("Updated successfully", new PermissionResDTO(updateEntity(existing)));
		permissionInterceptor.invalidateCache();
		return response;
	}

	@Transactional
	@Override
	public Response<PermissionResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "Permission not found"));
		permissionInterceptor.invalidateCache();
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<PermissionResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Permission not found"));
		permissionInterceptor.invalidateCache();
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<PermissionResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, PermissionResDTO.class);
	}

	private void validateForm(PermissionReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Name required");
		if (StringUtils.isBlank(reqDto.getAuthority())) returnErrorException("Authority required");
	}
}
