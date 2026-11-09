package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.CandidateCvReqDto;
import com.bd.erecruitment.dto.res.CandidateCvResDTO;
import com.bd.erecruitment.dto.res.GeneratedCvResDTO;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.service.impl.CandidateCvServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestApiController
@RequestMapping("/candidate-cv")
@RequiredArgsConstructor
@Tag(name = "4.0 Candidate CV", description = "Self-service access to the logged-in candidate's profile and generated CV")
public class CandidateCvController {

	private final CandidateCvServiceImpl candidateCvService;

	@Operation(summary = "Get my candidate profile")
	@GetMapping
	public Response<CandidateCvResDTO> getMyProfile() {
		return candidateCvService.getMyProfile();
	}

	@Operation(summary = "Update my candidate profile")
	@PutMapping
	public Response<CandidateCvResDTO> updateMyProfile(@RequestBody CandidateCvReqDto reqDto) {
		return candidateCvService.updateMyProfile(reqDto);
	}

	@Operation(summary = "Generate a CV/resume PDF from my profile data")
	@PostMapping("/generate-cv")
	public Response<GeneratedCvResDTO> generateCv() {
		return candidateCvService.generateCv();
	}

	@Operation(summary = "List my past CV generations")
	@GetMapping("/cv")
	public Response<GeneratedCvResDTO> listMyCvGenerations() {
		return candidateCvService.listMyCvGenerations();
	}

	@Operation(summary = "Download one of my generated CVs")
	@GetMapping("/cv/{id}/download")
	public ResponseEntity<byte[]> downloadMyCv(@PathVariable Long id) {
		StoredFile file = candidateCvService.downloadMyCv(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.getContentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
			.body(file.getData());
	}
}
