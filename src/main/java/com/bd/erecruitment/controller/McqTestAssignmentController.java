package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.AssignMcqTestReqDto;
import com.bd.erecruitment.dto.req.BulkAssignMcqTestReqDto;
import com.bd.erecruitment.dto.req.SubmitAnswerReqDto;
import com.bd.erecruitment.dto.res.McqTestAssignmentResDTO;
import com.bd.erecruitment.dto.res.McqTestAttemptQuestionDto;
import com.bd.erecruitment.service.impl.McqTestAssignmentServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestApiController
@RequestMapping("/mcq-test-assignment")
@RequiredArgsConstructor
@Tag(name = "5.3 MCQ Test Assignment", description = "Assigning tests to candidates and taking them")
public class McqTestAssignmentController {

	private final McqTestAssignmentServiceImpl assignmentService;

	@Operation(summary = "Assign a test to a candidate's application (recruiter/admin)")
	@PostMapping
	public Response<McqTestAssignmentResDTO> assign(@RequestBody AssignMcqTestReqDto reqDto) {
		return assignmentService.assign(reqDto);
	}

	@Operation(summary = "Bulk-assign a test to many candidates' applications at once, for a fixed exam schedule (recruiter/admin)")
	@PostMapping("/bulk")
	public Response<McqTestAssignmentResDTO> bulkAssign(@RequestBody BulkAssignMcqTestReqDto reqDto) {
		return assignmentService.bulkAssign(reqDto);
	}

	@Operation(summary = "My own assignments across all my applications (candidate)")
	@GetMapping("/my")
	public Response<McqTestAssignmentResDTO> myAssignments() {
		return assignmentService.myAssignments();
	}

	@Operation(summary = "Assignments for an application (owner or recruiter/admin)")
	@GetMapping("/by-application/{applicationId}")
	public Response<McqTestAssignmentResDTO> findByApplication(@PathVariable Long applicationId) {
		return assignmentService.findByApplication(applicationId);
	}

	@Operation(summary = "Find assignment by id")
	@GetMapping("/{id}")
	public Response<McqTestAssignmentResDTO> find(@PathVariable Long id) {
		return assignmentService.find(id);
	}

	@Operation(summary = "Questions for an assignment (candidate gets no answer key, staff gets full review)")
	@GetMapping("/{id}/questions")
	public Response<Object> getQuestions(@PathVariable Long id) {
		return assignmentService.getQuestions(id);
	}

	@Operation(summary = "Start (or resume) a test attempt (candidate only)")
	@PostMapping("/{id}/start")
	public Response<McqTestAssignmentResDTO> start(@PathVariable Long id) {
		return assignmentService.start(id);
	}

	@Operation(summary = "Save one answer (candidate only)")
	@PutMapping("/{id}/answer")
	public Response<McqTestAttemptQuestionDto> answer(@PathVariable Long id, @RequestBody SubmitAnswerReqDto reqDto) {
		return assignmentService.answer(id, reqDto);
	}

	@Operation(summary = "Move to the next question - forward-only, server-enforced (candidate only)")
	@PostMapping("/{id}/advance")
	public Response<McqTestAssignmentResDTO> advance(@PathVariable Long id) {
		return assignmentService.advance(id);
	}

	@Operation(summary = "Submit the test for grading (candidate only)")
	@PostMapping("/{id}/submit")
	public Response<McqTestAssignmentResDTO> submit(@PathVariable Long id) {
		return assignmentService.submit(id);
	}
}
