package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.JobCircularReqDto;
import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.service.BaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/e-recruitment/job-circular")
@Tag(name = "3.0 Job Circular", description = "API")
public class JobCircularController extends AbstractBaseController<JobCircularResDTO, JobCircularReqDto>{

	public JobCircularController(BaseService<JobCircularResDTO, JobCircularReqDto> service) {
		super(service);
	}
}
