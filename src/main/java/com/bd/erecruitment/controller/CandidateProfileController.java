package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.CandidateProfileReqDto;
import com.bd.erecruitment.dto.res.CandidateProfileResDTO;
import com.bd.erecruitment.dto.res.GeneratedCvResDTO;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.service.impl.CandidateProfileServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Self-service only, off "/candidate-profile" like ProfileController is off "/profile" - a
// candidate always manages their own profile/CV, never someone else's, so ownership is enforced
// in the service by the logged-in user's id rather than a path {id}.
@RestApiController
@RequestMapping("/candidate-profile")
@RequiredArgsConstructor
@Tag(name = "4.0 Candidate Profile", description = "Self-service access to the logged-in candidate's profile and generated CV")
public class CandidateProfileController {

	private final CandidateProfileServiceImpl candidateProfileService;

	@Operation(summary = "Get my candidate profile")
	@GetMapping
	public Response<CandidateProfileResDTO> getMyProfile() {
		return candidateProfileService.getMyProfile();
	}

	@Operation(summary = "Update my candidate profile")
	@PutMapping
	public Response<CandidateProfileResDTO> updateMyProfile(@RequestBody CandidateProfileReqDto reqDto) {
		return candidateProfileService.updateMyProfile(reqDto);
	}

	@Operation(summary = "Generate a CV/resume PDF from my profile data")
	@PostMapping("/generate-cv")
	public Response<GeneratedCvResDTO> generateCv() {
		return candidateProfileService.generateCv();
	}

	@Operation(summary = "List my past CV generations")
	@GetMapping("/cv")
	public Response<GeneratedCvResDTO> listMyCvGenerations() {
		return candidateProfileService.listMyCvGenerations();
	}

	@Operation(summary = "Download one of my generated CVs")
	@GetMapping("/cv/{id}/download")
	public ResponseEntity<byte[]> downloadMyCv(@PathVariable Long id) {
		StoredFile file = candidateProfileService.downloadMyCv(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.getContentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
			.body(file.getData());
	}
}
