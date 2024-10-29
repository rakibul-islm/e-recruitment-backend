package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.JobCircularReqDto;
import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.service.JobCircularService;
import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JobCircularServiceImpl extends AbstractBaseService<JobCircular> implements JobCircularService<JobCircularResDTO, JobCircularReqDto> {

	private JobCircularRepo jobCircularRepo;

	JobCircularServiceImpl(JobCircularRepo uRepo){
		super(uRepo);
		this.jobCircularRepo = uRepo;
	}

	@Override
	public Response<JobCircularResDTO> find(Long id) throws ServiceException {
		if(id == null) return getErrorResponse("Id required");

		Optional<JobCircular> o = jobCircularRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("JobCircular found", new JobCircularResDTO(o.get())) : getErrorResponse("JobCircular not found");
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> save(JobCircularReqDto reqDto) throws ServiceException{
		if(ValidateForm(reqDto) != null) return ValidateForm(reqDto);

		JobCircular jobCircular = reqDto.getBean();
		jobCircular = createEntity(jobCircular);

		if(jobCircular == null) return getErrorResponse("Can't Save Job Circular");

		return getSuccessResponse("Job Circular Saved Successfully", new JobCircularResDTO(jobCircular));
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> update(JobCircularReqDto reqDto) throws ServiceException {
		if(ValidateForm(reqDto) != null) return ValidateForm(reqDto);

		Optional<JobCircular> exOp = jobCircularRepo.findById(reqDto.getId());
		if(exOp.isEmpty()) return getErrorResponse("Job Circular not Found");

		JobCircular exJobCircular = exOp.get();
		BeanUtils.copyProperties(reqDto, exJobCircular);
		exJobCircular = updateEntity(exJobCircular);

		if(exJobCircular == null) return getErrorResponse("Can't Update Job Circular Info");

		return getSuccessResponse("Job Circular Updated Successfully", new JobCircularResDTO(exJobCircular));
	}

	@Override
	public Response<JobCircularResDTO> getAll(Pageable pageable, Boolean isPageable) throws ServiceException {
		if(Boolean.TRUE.equals(isPageable)) {
			Page<JobCircular> page = jobCircularRepo.findAllByDeleted(false, pageable);
			if(!page.hasContent()) return getErrorResponse("Job Circular not Found");

			return getSuccessResponse(
					"Found Job Circulars",
					page.map(data -> new ModelMapper().map(data, JobCircularResDTO.class))
			);
		}
		List<JobCircular> list = jobCircularRepo.findAllByDeleted(false);
		if(list == null || list.isEmpty()) return getErrorResponse("Job Circular not Found");

		return getSuccessResponse(
				"Found Job Circulars",
				list.stream().map(data -> new ModelMapper().map(data, JobCircularResDTO.class))
						.collect(Collectors.toList())
		);
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> delete(JobCircularReqDto reqDto) throws ServiceException {
		JobCircular z = reqDto.getBean();
		JobCircular exist = null;
		Optional<JobCircular> o = jobCircularRepo.findByIdAndDeleted(z.getId(), false);
		if(!o.isPresent()) return getErrorResponse("Job Circular not Found");
		exist = o.get();
		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete Successfully");
	}

	@Transactional
	@Override
	public Response<JobCircularResDTO> remove(Long id) throws ServiceException {
		JobCircular exist = null;
		Optional<JobCircular> o = jobCircularRepo.findByIdAndDeleted(id, false);
		if(o.isEmpty()) return getErrorResponse("Job Circular not Found");
		exist = o.get();
		try {
			removeEntityById(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete Successfully");
	}

	public Response<JobCircularResDTO> ValidateForm(JobCircularReqDto reqDto) throws ServiceException {
		// validation
		if(StringUtils.isBlank(reqDto.getJobTitle())) return getErrorResponse("Job Title Required");
		if(StringUtils.isBlank(reqDto.getCompanyName())) return getErrorResponse("JCompany Name Required");
		if(StringUtils.isBlank(reqDto.getCompanyPhone())) return getErrorResponse("Company Phone Required");
		if(StringUtils.isBlank(reqDto.getCompanyEmail())) return getErrorResponse("Company Email Required");
		if(StringUtils.isBlank(reqDto.getSalary())) return getErrorResponse("Salary Required");
		if(StringUtils.isBlank(reqDto.getJobRequirement())) return getErrorResponse("Job Requirement Required");
		if(Objects.isNull(reqDto.getVacancy()) || reqDto.getVacancy() < 1) return getErrorResponse("Vacancy Required");
		if(Objects.isNull(reqDto.getApplicationDeadLine())) return getErrorResponse("Application DeadLine Required");

		return null;
	}
	
}
