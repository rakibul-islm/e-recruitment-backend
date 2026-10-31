package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.RestoreReqDTO;
import com.bd.erecruitment.dto.res.RestoreStatusResDTO;
import com.bd.erecruitment.service.impl.DatabaseRestoreServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Kept under /backup-restore, not /backup, so restoring needs its own authority rather than backup:write.
@RestApiController
@RequestMapping("/backup-restore")
@RequiredArgsConstructor
@Tag(name = "Database Restore", description = "Replace all data with the contents of a backup")
public class DatabaseRestoreController implements ResponseWrapper {

	private final DatabaseRestoreServiceImpl restoreService;

	@GetMapping("/status")
	@Operation(summary = "Progress and outcome of the latest restore")
	public ResponseEntity<Response<RestoreStatusResDTO>> status() {
		return respond(restoreService.status());
	}

	@PostMapping("/{id}")
	@Operation(summary = "Restore a backup in the background; body must carry confirmation=RESTORE")
	public ResponseEntity<Response<RestoreStatusResDTO>> restore(@PathVariable Long id, @RequestBody RestoreReqDTO request) {
		return respond(restoreService.start(id, request.confirmation()));
	}
}
