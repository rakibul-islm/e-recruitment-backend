package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.ApplicationStatusChangeReqDto;
import com.bd.erecruitment.dto.req.ApplyReqDto;
import com.bd.erecruitment.dto.res.ApplicationResDTO;
import com.bd.erecruitment.dto.res.ApplicationStatusHistoryResDTO;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.service.impl.ApplicationServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestApiController
@RequestMapping("/application")
@RequiredArgsConstructor
@Tag(name = "4.1 Application", description = "Candidate apply/track flow and recruiter application management")
public class ApplicationController {

	private static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "isPageable");

	private final ApplicationServiceImpl applicationService;

	@Operation(summary = "Apply to a job")
	@PostMapping
	public Response<ApplicationResDTO> apply(@RequestBody ApplyReqDto reqDto) {
		return applicationService.apply(reqDto);
	}

	@Operation(summary = "My applications")
	@GetMapping("/my")
	public Response<ApplicationResDTO> myApplications() {
		return applicationService.getMyApplications();
	}

	@Operation(summary = "Filter applications (recruiter/admin)")
	@GetMapping("/filter")
	public Response<ApplicationResDTO> filter(@RequestParam Map<String, String> filters, @Nullable Pageable pageable,
			@RequestParam(required = false) Boolean isPageable) {
		Map<String, String> cleanFilters = new HashMap<>(filters);
		RESERVED_PARAMS.forEach(cleanFilters::remove);
		return applicationService.filter(cleanFilters, pageable, isPageable);
	}

	@Operation(summary = "Find application by id")
	@GetMapping("/{id}")
	public Response<ApplicationResDTO> find(@PathVariable Long id) {
		return applicationService.find(id);
	}

	@Operation(summary = "Change application status (recruiter/admin)")
	@PutMapping("/{id}/status")
	public Response<ApplicationResDTO> changeStatus(@PathVariable Long id, @RequestBody ApplicationStatusChangeReqDto reqDto) {
		return applicationService.changeStatus(id, reqDto);
	}

	@Operation(summary = "Status change history for an application")
	@GetMapping("/{id}/history")
	public Response<ApplicationStatusHistoryResDTO> history(@PathVariable Long id) {
		return applicationService.getHistory(id);
	}

	@Operation(summary = "Download the CV attached to an application")
	@GetMapping("/{id}/cv")
	public ResponseEntity<byte[]> downloadCv(@PathVariable Long id) {
		StoredFile file = applicationService.downloadCv(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.getContentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
			.body(file.getData());
	}
}
