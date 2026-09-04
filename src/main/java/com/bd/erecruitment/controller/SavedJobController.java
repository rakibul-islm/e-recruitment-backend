package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.res.SavedJobResDTO;
import com.bd.erecruitment.service.impl.SavedJobServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestApiController
@RequestMapping("/saved-job")
@RequiredArgsConstructor
@Tag(name = "4.2 Saved Jobs", description = "Candidate job bookmarks")
public class SavedJobController {

	private final SavedJobServiceImpl savedJobService;

	@Operation(summary = "Save/unsave a job (toggle)")
	@PostMapping("/toggle/{jobCircularId}")
	public Response<Map<String, Boolean>> toggle(@PathVariable Long jobCircularId) {
		return savedJobService.toggle(jobCircularId);
	}

	@Operation(summary = "My saved jobs")
	@GetMapping("/my")
	public Response<SavedJobResDTO> myList() {
		return savedJobService.myList();
	}
}
