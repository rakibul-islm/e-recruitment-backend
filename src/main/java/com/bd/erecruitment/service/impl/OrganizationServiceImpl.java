package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.OrganizationReqDto;
import com.bd.erecruitment.dto.res.OrganizationResDTO;
import com.bd.erecruitment.entity.Organization;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.OrganizationRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrganizationServiceImpl extends AbstractBaseService<Organization> implements BaseService<OrganizationResDTO, OrganizationReqDto> {

	public OrganizationServiceImpl(OrganizationRepo organizationRepo) {
		super(organizationRepo);
	}

	@Override
	public Response<OrganizationResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		Organization organization = findByIdOrThrow(id, "Organization not found");
		if (isScopedRecruiter() && !organizationMatchesCaller(organization.getId())) returnNotFoundException("Organization not found");
		return getSuccessResponse("Organization found", new OrganizationResDTO(organization));
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> save(OrganizationReqDto reqDto) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot create organizations");
		validateForm(reqDto);
		Organization organization = createEntity(reqDto.getBean());
		return getCreatedResponse("Organization saved successfully", new OrganizationResDTO(organization));
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> update(OrganizationReqDto reqDto) {
		if (isScopedRecruiter() && !organizationMatchesCaller(reqDto.getId()))
			throw new ForbiddenException("You may only edit your own organization");
		validateForm(reqDto);
		Organization existing = findByIdOrThrow(reqDto.getId(), "Organization not found");
		modelMapper.map(reqDto, existing);
		existing = updateEntity(existing);
		return getSuccessResponse("Organization updated successfully", new OrganizationResDTO(existing));
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> delete(Long id) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot delete organizations");
		deleteEntity(findByIdOrThrow(id, "Organization not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> remove(Long id) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot delete organizations");
		removeEntity(findByIdOrThrow(id, "Organization not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<OrganizationResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		if (isScopedRecruiter()) {
			MyUserDetail me = getLoggedInUserDetails();
			filters = new HashMap<>(filters);
			filters.put("id", me.getOrganizationId() == null ? "-1" : String.valueOf(me.getOrganizationId()));
		}
		return genericFilter(filters, pageable, isPageable, OrganizationResDTO.class);
	}

	private boolean organizationMatchesCaller(Long organizationId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getOrganizationId() != null && me.getOrganizationId().equals(organizationId);
	}

	private void validateForm(OrganizationReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Organization name required");
	}
}
