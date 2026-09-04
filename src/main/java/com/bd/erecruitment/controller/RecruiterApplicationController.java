package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.RecruiterApplicationRejectReqDto;
import com.bd.erecruitment.dto.req.RecruiterApplicationReqDto;
import com.bd.erecruitment.dto.res.RecruiterApplicationResDTO;
import com.bd.erecruitment.service.impl.RecruiterApplicationServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/recruiter-application")
@Tag(name = "3.2 Recruiter Application", description = "Public employer/recruiter access requests, reviewed by an admin before a staff account is created")
public class RecruiterApplicationController extends AbstractBaseController<RecruiterApplicationResDTO, RecruiterApplicationReqDto> {

	private final RecruiterApplicationServiceImpl recruiterApplicationService;

	public RecruiterApplicationController(RecruiterApplicationServiceImpl service) {
		super(service);
		this.recruiterApplicationService = service;
	}

	@PutMapping("/{id}/approve")
	@Operation(summary = "Approve a pending application - creates the recruiter's staff account (admin/manager)")
	public ResponseEntity<Response<RecruiterApplicationResDTO>> approve(@PathVariable Long id) {
		return respond(recruiterApplicationService.approve(id));
	}

	@PutMapping("/{id}/reject")
	@Operation(summary = "Reject a pending application, with an optional note (admin/manager)")
	public ResponseEntity<Response<RecruiterApplicationResDTO>> reject(@PathVariable Long id, @RequestBody RecruiterApplicationRejectReqDto reqDto) {
		return respond(recruiterApplicationService.reject(id, reqDto));
	}
}
