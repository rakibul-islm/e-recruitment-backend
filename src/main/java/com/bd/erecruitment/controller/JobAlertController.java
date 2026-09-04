package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.JobAlertReqDto;
import com.bd.erecruitment.dto.res.JobAlertResDTO;
import com.bd.erecruitment.service.impl.JobAlertServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestApiController
@RequestMapping("/job-alert")
@RequiredArgsConstructor
@Tag(name = "4.3 Job Alerts", description = "Candidate saved-search email alerts")
public class JobAlertController {

	private final JobAlertServiceImpl jobAlertService;

	@Operation(summary = "Create or update a job alert")
	@PostMapping
	public Response<JobAlertResDTO> save(@RequestBody JobAlertReqDto reqDto) {
		return jobAlertService.save(reqDto);
	}

	@Operation(summary = "My job alerts")
	@GetMapping("/my")
	public Response<JobAlertResDTO> myList() {
		return jobAlertService.myList();
	}

	@Operation(summary = "Remove a job alert")
	@DeleteMapping("/{id}")
	public Response<JobAlertResDTO> remove(@PathVariable Long id) {
		return jobAlertService.remove(id);
	}
}
