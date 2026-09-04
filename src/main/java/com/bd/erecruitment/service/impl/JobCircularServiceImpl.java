package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.JobCircularReqDto;
import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.entity.Company;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.CompanyRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// A plain recruiter (isScopedRecruiter(), from AbstractBaseService) may only see/manage job
// postings that belong to their own company (User.companyId) - same rule and same reasoning as
// CompanyServiceImpl. The company fields on a scoped recruiter's job posting are always
// re-derived from their actual Company row server-side, never trusted from the request, so a
// tampered/disabled frontend field can't post a job under a different company.
@Service
public class JobCircularServiceImpl extends AbstractBaseService<JobCircular> implements BaseService<JobCircularResDTO, JobCircularReqDto> {

	private final CompanyRepo companyRepo;

	public JobCircularServiceImpl(JobCircularRepo jobCircularRepo, CompanyRepo companyRepo) {
		super(jobCircularRepo);
		this.companyRepo = companyRepo;
	}

	@Override
	public Response<JobCircularResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		JobCircular jobCircular = findByIdOrThrow(id, "Job circular not found");
		if (isScopedRecruiter() && !companyMatchesCaller(jobCircular.getCompanyId())) returnNotFoundException("Job circular not found");
		return getSuccessResponse("Job circular found", new JobCircularResDTO(jobCircular));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> save(JobCircularReqDto reqDto) {
		if (isScopedRecruiter()) applyOwnCompany(reqDto);
		validateForm(reqDto);
		JobCircular bean = reqDto.getBean();
		if (StringUtils.isBlank(bean.getStatus())) bean.setStatus("DRAFT");
		JobCircular jobCircular = createEntity(bean);
		return getCreatedResponse("Job circular saved successfully", new JobCircularResDTO(jobCircular));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> update(JobCircularReqDto reqDto) {
		JobCircular existing = findByIdOrThrow(reqDto.getId(), "Job circular not found");
		if (isScopedRecruiter()) {
			if (!companyMatchesCaller(existing.getCompanyId())) throw new ForbiddenException("You may only manage your own company's job postings");
			applyOwnCompany(reqDto);
		}
		validateForm(reqDto);
		modelMapper.map(reqDto, existing);
		existing = updateEntity(existing);
		return getSuccessResponse("Job circular updated successfully", new JobCircularResDTO(existing));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> delete(Long id) {
		JobCircular jobCircular = findByIdOrThrow(id, "Job circular not found");
		if (isScopedRecruiter() && !companyMatchesCaller(jobCircular.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's job postings");
		deleteEntity(jobCircular);
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> remove(Long id) {
		JobCircular jobCircular = findByIdOrThrow(id, "Job circular not found");
		if (isScopedRecruiter() && !companyMatchesCaller(jobCircular.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's job postings");
		removeEntity(jobCircular);
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<JobCircularResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		if (isScopedRecruiter()) {
			MyUserDetail me = getLoggedInUserDetails();
			filters = new HashMap<>(filters);
			// No company linked - an unrestricted filter would otherwise show every job posting.
			filters.put("companyId", me.getCompanyId() == null ? "-1" : String.valueOf(me.getCompanyId()));
		}
		return genericFilter(filters, pageable, isPageable, JobCircularResDTO.class);
	}

	// Re-derives companyId/companyName/companyAddress/companyWebsite/companyPhone/companyEmail
	// from the recruiter's own Company row, overriding whatever the request sent for them.
	private void applyOwnCompany(JobCircularReqDto reqDto) {
		MyUserDetail me = getLoggedInUserDetails();
		if (me.getCompanyId() == null) throw new ForbiddenException("Your account isn't linked to a company yet");
		Company company = companyRepo.findByIdAndDeleted(me.getCompanyId(), false)
			.orElseThrow(() -> new ForbiddenException("Your linked company could not be found"));
		reqDto.setCompanyId(company.getId());
		reqDto.setCompanyName(company.getName());
		reqDto.setCompanyAddress(company.getAddress());
		reqDto.setCompanyWebsite(company.getWebsite());
		reqDto.setCompanyPhone(company.getPhone());
		reqDto.setCompanyEmail(company.getEmail());
	}

	private boolean companyMatchesCaller(Long companyId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getCompanyId() != null && me.getCompanyId().equals(companyId);
	}

	private void validateForm(JobCircularReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getJobTitle())) returnErrorException("Job title required");
		if (StringUtils.isBlank(reqDto.getCompanyName())) returnErrorException("Company name required");
		if (StringUtils.isBlank(reqDto.getCompanyPhone())) returnErrorException("Company phone required");
		if (StringUtils.isBlank(reqDto.getCompanyEmail())) returnErrorException("Company email required");
		if (StringUtils.isBlank(reqDto.getSalary())) returnErrorException("Salary required");
		if (StringUtils.isBlank(reqDto.getJobRequirement())) returnErrorException("Job requirement required");
		if (Objects.isNull(reqDto.getVacancy()) || reqDto.getVacancy() < 1) returnErrorException("Vacancy required");
		if (Objects.isNull(reqDto.getApplicationDeadLine())) returnErrorException("Application deadline required");
	}
}
