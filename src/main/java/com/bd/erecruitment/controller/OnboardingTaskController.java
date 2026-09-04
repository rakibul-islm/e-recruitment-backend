package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.OnboardingTaskReqDto;
import com.bd.erecruitment.dto.res.OnboardingTaskResDTO;
import com.bd.erecruitment.service.impl.OnboardingServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestApiController
@RequestMapping("/onboarding-task")
@RequiredArgsConstructor
@Tag(name = "5.2 Onboarding", description = "Post-hire onboarding checklist")
public class OnboardingTaskController {

	private final OnboardingServiceImpl onboardingService;

	@Operation(summary = "Add an onboarding task (recruiter/admin)")
	@PostMapping
	public Response<OnboardingTaskResDTO> addTask(@RequestBody OnboardingTaskReqDto reqDto) {
		return onboardingService.addTask(reqDto);
	}

	@Operation(summary = "Onboarding tasks for an application (owner or recruiter/admin)")
	@GetMapping("/by-application/{applicationId}")
	public Response<OnboardingTaskResDTO> findByApplication(@PathVariable Long applicationId) {
		return onboardingService.findByApplication(applicationId);
	}

	@Operation(summary = "Mark a task complete")
	@PutMapping("/{id}/complete")
	public Response<OnboardingTaskResDTO> complete(@PathVariable Long id) {
		return onboardingService.complete(id);
	}

	@Operation(summary = "Remove an onboarding task (recruiter/admin)")
	@DeleteMapping("/{id}")
	public Response<OnboardingTaskResDTO> remove(@PathVariable Long id) {
		return onboardingService.remove(id);
	}
}
