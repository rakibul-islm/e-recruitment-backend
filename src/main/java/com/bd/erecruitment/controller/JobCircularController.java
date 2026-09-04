package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.JobCircularReqDto;
import com.bd.erecruitment.dto.req.JobPostingAiSuggestReqDto;
import com.bd.erecruitment.dto.res.JobCircularResDTO;
import com.bd.erecruitment.dto.res.JobPostingAiSuggestResDTO;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.impl.JobPostingAiServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/job-circular")
@Tag(name = "3.0 Job Circular", description = "API")
public class JobCircularController extends AbstractBaseController<JobCircularResDTO, JobCircularReqDto> {

	private final JobPostingAiServiceImpl jobPostingAiService;

	public JobCircularController(BaseService<JobCircularResDTO, JobCircularReqDto> service, JobPostingAiServiceImpl jobPostingAiService) {
		super(service);
		this.jobPostingAiService = jobPostingAiService;
	}

	@Operation(summary = "Generate AI-suggested job posting content from a job title and context")
	@PostMapping("/ai-suggest")
	public Response<JobPostingAiSuggestResDTO> aiSuggest(@RequestBody JobPostingAiSuggestReqDto req) {
		return jobPostingAiService.suggest(req);
	}
}
