package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.JobCircularReqDto;
import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.entity.Organization;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.OrganizationRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class JobCircularServiceImpl extends AbstractBaseService<JobCircular> implements BaseService<JobCircularResDTO, JobCircularReqDto> {

	private static final String STATUS_PUBLISHED = "PUBLISHED";

	private final OrganizationRepo organizationRepo;

	public JobCircularServiceImpl(JobCircularRepo jobCircularRepo, OrganizationRepo organizationRepo) {
		super(jobCircularRepo);
		this.organizationRepo = organizationRepo;
	}

	@Override
	public Response<JobCircularResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		JobCircular jobCircular = findByIdOrThrow(id, "Job circular not found");
		if (isScopedRecruiter() && !organizationMatchesCaller(jobCircular.getOrganizationId())) returnNotFoundException("Job circular not found");
		return getSuccessResponse("Job circular found", new JobCircularResDTO(jobCircular));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> save(JobCircularReqDto reqDto) {
		if (isScopedRecruiter()) applyOwnOrganization(reqDto);
		validateForm(reqDto);
		JobCircular bean = reqDto.getBean();
		if (StringUtils.isBlank(bean.getStatus())) bean.setStatus("DRAFT");
		if (STATUS_PUBLISHED.equals(bean.getStatus())) bean.setPublishedOn(new Date());
		JobCircular jobCircular = createEntity(bean);
		return getCreatedResponse("Job circular saved successfully", new JobCircularResDTO(jobCircular));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> update(JobCircularReqDto reqDto) {
		JobCircular existing = findByIdOrThrow(reqDto.getId(), "Job circular not found");
		if (isScopedRecruiter()) {
			if (!organizationMatchesCaller(existing.getOrganizationId())) throw new ForbiddenException("You may only manage your own organization's job postings");
			applyOwnOrganization(reqDto);
		}
		validateForm(reqDto);
		boolean wasPublished = STATUS_PUBLISHED.equals(existing.getStatus());
		modelMapper.map(reqDto, existing);
		if (!wasPublished && STATUS_PUBLISHED.equals(existing.getStatus())) existing.setPublishedOn(new Date());
		existing = updateEntity(existing);
		return getSuccessResponse("Job circular updated successfully", new JobCircularResDTO(existing));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> delete(Long id) {
		JobCircular jobCircular = findByIdOrThrow(id, "Job circular not found");
		if (isScopedRecruiter() && !organizationMatchesCaller(jobCircular.getOrganizationId()))
			throw new ForbiddenException("You may only manage your own organization's job postings");
		deleteEntity(jobCircular);
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> remove(Long id) {
		JobCircular jobCircular = findByIdOrThrow(id, "Job circular not found");
		if (isScopedRecruiter() && !organizationMatchesCaller(jobCircular.getOrganizationId()))
			throw new ForbiddenException("You may only manage your own organization's job postings");
		removeEntity(jobCircular);
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<JobCircularResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		if (isScopedRecruiter()) {
			MyUserDetail me = getLoggedInUserDetails();
			filters = new HashMap<>(filters);
			filters.put("organizationId", me.getOrganizationId() == null ? "-1" : String.valueOf(me.getOrganizationId()));
		}
		return genericFilter(filters, pageable, isPageable, JobCircularResDTO.class);
	}

	private void applyOwnOrganization(JobCircularReqDto reqDto) {
		MyUserDetail me = getLoggedInUserDetails();
		if (me.getOrganizationId() == null) throw new ForbiddenException("Your account isn't linked to an organization yet");
		Organization organization = organizationRepo.findByIdAndDeleted(me.getOrganizationId(), false)
			.orElseThrow(() -> new ForbiddenException("Your linked organization could not be found"));
		reqDto.setOrganizationId(organization.getId());
		reqDto.setOrganizationName(organization.getName());
		reqDto.setOrganizationAddress(organization.getAddress());
		reqDto.setOrganizationWebsite(organization.getWebsite());
		reqDto.setOrganizationPhone(organization.getPhone());
		reqDto.setOrganizationEmail(organization.getEmail());
	}

	private boolean organizationMatchesCaller(Long organizationId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getOrganizationId() != null && me.getOrganizationId().equals(organizationId);
	}

	private void validateForm(JobCircularReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getJobTitle())) returnErrorException("Job title required");
		if (StringUtils.isBlank(reqDto.getOrganizationName())) returnErrorException("Organization name required");
		if (StringUtils.isBlank(reqDto.getOrganizationPhone())) returnErrorException("Organization phone required");
		if (StringUtils.isBlank(reqDto.getOrganizationEmail())) returnErrorException("Organization email required");
		if (StringUtils.isBlank(reqDto.getSalary())) returnErrorException("Salary required");
		if (StringUtils.isBlank(reqDto.getJobRequirement())) returnErrorException("Job requirement required");
		if (Objects.isNull(reqDto.getVacancy()) || reqDto.getVacancy() < 1) returnErrorException("Vacancy required");
		if (Objects.isNull(reqDto.getApplicationDeadLine())) returnErrorException("Application deadline required");
	}
}
