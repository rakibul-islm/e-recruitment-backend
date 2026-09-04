package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.CompanyTypeReqDto;
import com.bd.erecruitment.dto.res.CompanyTypeResDTO;
import com.bd.erecruitment.entity.CompanyType;
import com.bd.erecruitment.repository.CompanyTypeRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CompanyTypeServiceImpl extends AbstractBaseService<CompanyType> implements BaseService<CompanyTypeResDTO, CompanyTypeReqDto> {

	private final CompanyTypeRepo companyTypeRepo;

	public CompanyTypeServiceImpl(CompanyTypeRepo companyTypeRepo) {
		super(companyTypeRepo);
		this.companyTypeRepo = companyTypeRepo;
	}

	@Override
	public Response<CompanyTypeResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Company type found", new CompanyTypeResDTO(findByIdOrThrow(id, "Company type not found")));
	}

	@Transactional
	@Override
	public Response<CompanyTypeResDTO> save(CompanyTypeReqDto reqDto) {
		validateForm(reqDto);
		companyTypeRepo.findFirstByNameIgnoreCaseAndDeleted(reqDto.getName(), false)
			.ifPresent(existing -> returnErrorException("This company type already exists"));

		CompanyType companyType = createEntity(reqDto.getBean());
		return getCreatedResponse("Company type saved successfully", new CompanyTypeResDTO(companyType));
	}

	@Transactional
	@Override
	public Response<CompanyTypeResDTO> update(CompanyTypeReqDto reqDto) {
		validateForm(reqDto);
		CompanyType existing = findByIdOrThrow(reqDto.getId(), "Company type not found");
		modelMapper.map(reqDto, existing);
		existing = updateEntity(existing);
		return getSuccessResponse("Company type updated successfully", new CompanyTypeResDTO(existing));
	}

	@Transactional
	@Override
	public Response<CompanyTypeResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "Company type not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<CompanyTypeResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Company type not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<CompanyTypeResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, CompanyTypeResDTO.class);
	}

	private void validateForm(CompanyTypeReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Company type name required");
	}
}
