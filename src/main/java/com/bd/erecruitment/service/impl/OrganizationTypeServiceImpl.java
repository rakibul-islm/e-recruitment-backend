package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.OrganizationTypeReqDto;
import com.bd.erecruitment.dto.res.OrganizationTypeResDTO;
import com.bd.erecruitment.entity.OrganizationType;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.OrganizationTypeRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrganizationTypeServiceImpl extends AbstractBaseService<OrganizationType> implements BaseService<OrganizationTypeResDTO, OrganizationTypeReqDto> {

	private final OrganizationTypeRepo organizationTypeRepo;

	public OrganizationTypeServiceImpl(OrganizationTypeRepo organizationTypeRepo) {
		super(organizationTypeRepo);
		this.organizationTypeRepo = organizationTypeRepo;
	}

	@Override
	public Response<OrganizationTypeResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Organization type found", new OrganizationTypeResDTO(findByIdOrThrow(id, "Organization type not found")));
	}

	@Transactional
	@Override
	public Response<OrganizationTypeResDTO> save(OrganizationTypeReqDto reqDto) {
		validateForm(reqDto);
		organizationTypeRepo.findFirstByNameIgnoreCaseAndDeleted(reqDto.getName(), false)
			.ifPresent(existing -> returnErrorException("This organization type already exists"));

		OrganizationType organizationType = createEntity(reqDto.getBean(), currentActorOrSystem());
		return getCreatedResponse("Organization type saved successfully", new OrganizationTypeResDTO(organizationType));
	}

	private String currentActorOrSystem() {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null ? me.getUsername() : "system";
	}

	@Transactional
	@Override
	public Response<OrganizationTypeResDTO> update(OrganizationTypeReqDto reqDto) {
		validateForm(reqDto);
		OrganizationType existing = findByIdOrThrow(reqDto.getId(), "Organization type not found");
		modelMapper.map(reqDto, existing);
		existing = updateEntity(existing);
		return getSuccessResponse("Organization type updated successfully", new OrganizationTypeResDTO(existing));
	}

	@Transactional
	@Override
	public Response<OrganizationTypeResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "Organization type not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<OrganizationTypeResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Organization type not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<OrganizationTypeResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, OrganizationTypeResDTO.class);
	}

	private void validateForm(OrganizationTypeReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Organization type name required");
	}
}
