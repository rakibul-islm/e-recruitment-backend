package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.CompanyReqDto;
import com.bd.erecruitment.dto.res.CompanyResDTO;
import com.bd.erecruitment.entity.Company;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.CompanyRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// A plain recruiter (isScopedRecruiter(), from AbstractBaseService) is scoped to their own
// company via User.companyId, set when their RecruiterApplication is approved. Every entry point
// re-checks this server-side - the frontend disabling/pre-filling the company field is a UX
// nicety, not the actual access boundary.
@Service
public class CompanyServiceImpl extends AbstractBaseService<Company> implements BaseService<CompanyResDTO, CompanyReqDto> {

	public CompanyServiceImpl(CompanyRepo companyRepo) {
		super(companyRepo);
	}

	@Override
	public Response<CompanyResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		Company company = findByIdOrThrow(id, "Company not found");
		if (isScopedRecruiter() && !companyMatchesCaller(company.getId())) returnNotFoundException("Company not found");
		return getSuccessResponse("Company found", new CompanyResDTO(company));
	}

	@Transactional
	@Override
	public Response<CompanyResDTO> save(CompanyReqDto reqDto) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot create companies");
		validateForm(reqDto);
		Company company = createEntity(reqDto.getBean());
		return getCreatedResponse("Company saved successfully", new CompanyResDTO(company));
	}

	@Transactional
	@Override
	public Response<CompanyResDTO> update(CompanyReqDto reqDto) {
		if (isScopedRecruiter() && !companyMatchesCaller(reqDto.getId()))
			throw new ForbiddenException("You may only edit your own company");
		validateForm(reqDto);
		Company existing = findByIdOrThrow(reqDto.getId(), "Company not found");
		modelMapper.map(reqDto, existing);
		existing = updateEntity(existing);
		return getSuccessResponse("Company updated successfully", new CompanyResDTO(existing));
	}

	@Transactional
	@Override
	public Response<CompanyResDTO> delete(Long id) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot delete companies");
		deleteEntity(findByIdOrThrow(id, "Company not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<CompanyResDTO> remove(Long id) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot delete companies");
		removeEntity(findByIdOrThrow(id, "Company not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<CompanyResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		if (isScopedRecruiter()) {
			MyUserDetail me = getLoggedInUserDetails();
			filters = new HashMap<>(filters);
			// No company linked - an unrestricted filter would otherwise show every company.
			filters.put("id", me.getCompanyId() == null ? "-1" : String.valueOf(me.getCompanyId()));
		}
		return genericFilter(filters, pageable, isPageable, CompanyResDTO.class);
	}

	private boolean companyMatchesCaller(Long companyId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getCompanyId() != null && me.getCompanyId().equals(companyId);
	}

	private void validateForm(CompanyReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Company name required");
	}
}
