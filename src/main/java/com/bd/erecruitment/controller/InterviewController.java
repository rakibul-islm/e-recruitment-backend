package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.InterviewFeedbackReqDto;
import com.bd.erecruitment.dto.req.ScheduleInterviewReqDto;
import com.bd.erecruitment.dto.res.InterviewResDTO;
import com.bd.erecruitment.service.impl.InterviewServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestApiController
@RequestMapping("/interview")
@RequiredArgsConstructor
@Tag(name = "5.0 Interview", description = "Interview scheduling and feedback")
public class InterviewController {

	private static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "isPageable");

	private final InterviewServiceImpl interviewService;

	@Operation(summary = "Schedule an interview (recruiter/admin)")
	@PostMapping
	public Response<InterviewResDTO> schedule(@RequestBody ScheduleInterviewReqDto reqDto) {
		return interviewService.schedule(reqDto);
	}

	@Operation(summary = "Filter interviews (recruiter/admin)")
	@GetMapping("/filter")
	public Response<InterviewResDTO> filter(@RequestParam Map<String, String> filters, @Nullable Pageable pageable,
			@RequestParam(required = false) Boolean isPageable) {
		Map<String, String> cleanFilters = new HashMap<>(filters);
		RESERVED_PARAMS.forEach(cleanFilters::remove);
		return interviewService.filter(cleanFilters, pageable, isPageable);
	}

	@Operation(summary = "Find interview by id")
	@GetMapping("/{id}")
	public Response<InterviewResDTO> find(@PathVariable Long id) {
		return interviewService.find(id);
	}

	@Operation(summary = "Interviews for an application (owner or recruiter/admin)")
	@GetMapping("/by-application/{applicationId}")
	public Response<InterviewResDTO> findByApplication(@PathVariable Long applicationId) {
		return interviewService.findByApplication(applicationId);
	}

	@Operation(summary = "Cancel/complete an interview (recruiter/admin)")
	@PutMapping("/{id}/status")
	public Response<InterviewResDTO> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
		return interviewService.changeStatus(id, body.get("status"));
	}

	@Operation(summary = "Submit interviewer feedback")
	@PostMapping("/{id}/feedback")
	public Response<InterviewResDTO> submitFeedback(@PathVariable Long id, @RequestBody InterviewFeedbackReqDto reqDto) {
		return interviewService.submitFeedback(id, reqDto);
	}
}
