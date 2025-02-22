package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.JobCircularReqDto;
import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.JobCircularService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@RestApiController
@RequestMapping("/e-recruitment/job-circular")
@Tag(name = "3.0 Job Circular", description = "API")
public class JobCircularController extends AbstractBaseController<JobCircularResDTO, JobCircularReqDto>{
	private JobCircularService<JobCircularResDTO, JobCircularReqDto> jobCircularService;

	public JobCircularController(BaseService<JobCircularResDTO, JobCircularReqDto> service, JobCircularService<JobCircularResDTO, JobCircularReqDto> jobCircularService) {
		super(service);
        this.jobCircularService = jobCircularService;
    }

	@Operation(summary = "Filter Job Circular")
	@GetMapping("/filter")
	public Response<JobCircularResDTO> filter(
			@Nullable Pageable pageable,
			@RequestParam(required = false) Boolean isPageable,
			@RequestParam(required = false) String jobTitle,
			@RequestParam(required = false) String companyName,
			@RequestParam(required = false) String companyAddress,
			@RequestParam(required = false) String companyPhone,
			@RequestParam(required = false) String companyEmail,
			@RequestParam(required = false) String companyWebsite,
			@RequestParam(required = false) String companyBusiness,
			@RequestParam(required = false)
				@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
				Date applicationDeadLine,
			@RequestParam(required = false) Integer vacancy,
			@RequestParam(required = false) String experience,
			@RequestParam(required = false) String salary,
			@RequestParam(required = false) String jobLocation,
			@RequestParam(required = false) String jobRequirement,
			@RequestParam(required = false) String jobResponsibilities,
			@RequestParam(required = false) String otherBenefits,
			@RequestParam(required = false) String workPlace,
			@RequestParam(required = false) String employmentStatus
	) throws Exception {
		return jobCircularService.filter(
				pageable,
				isPageable,
				jobTitle,
				companyName,
				companyAddress,
				companyPhone,
				companyEmail,
				companyWebsite,
				companyBusiness,
				applicationDeadLine,
				vacancy,
				experience,
				salary,
				jobLocation,
				jobRequirement,
				jobResponsibilities,
				otherBenefits,
				workPlace,
				employmentStatus);
	}
}
