package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.JobCircularReqDto;
import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.ServiceRepository;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;

@Service
public class JobCircularServiceImpl extends AbstractBaseService<JobCircular> implements BaseService<JobCircularResDTO, JobCircularReqDto> {

    public JobCircularServiceImpl(JobCircularRepo jobCircularRepo) {
        super(jobCircularRepo);
    }

    @Override
	public Response<JobCircularResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		JobCircular jobCircular = findByIdOrThrow(id, "Job circular not found");
		return getSuccessResponse("Job circular found", new JobCircularResDTO(jobCircular));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> save(JobCircularReqDto reqDto) {
		validateForm(reqDto);
		JobCircular jobCircular = createEntity(reqDto.getBean());
		return getCreatedResponse("Job circular saved successfully", new JobCircularResDTO(jobCircular));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> update(JobCircularReqDto reqDto) {
		validateForm(reqDto);
		JobCircular existing = findByIdOrThrow(reqDto.getId(), "Job circular not found");
		modelMapper.map(reqDto, existing);
		existing = updateEntity(existing);
		return getSuccessResponse("Job circular updated successfully", new JobCircularResDTO(existing));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "Job circular not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Job circular not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<JobCircularResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, JobCircularResDTO.class);
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
