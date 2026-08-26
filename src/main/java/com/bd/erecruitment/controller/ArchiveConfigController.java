package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.ArchiveConfigReqDto;
import com.bd.erecruitment.dto.res.ArchiveConfigResDTO;
import com.bd.erecruitment.dto.res.ArchivedDataResDTO;
import com.bd.erecruitment.service.impl.ArchiveConfigServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestApiController
@RequestMapping("/archive-config")
@Tag(name = "Archive Config")
public class ArchiveConfigController extends AbstractBaseController<ArchiveConfigResDTO, ArchiveConfigReqDto> {

	private final ArchiveConfigServiceImpl archiveConfigService;

	ArchiveConfigController(ArchiveConfigServiceImpl service) {
		super(service);
		this.archiveConfigService = service;
	}

	@GetMapping("/source-tables")
	@Operation(summary = "List tables in the app's schema, for the source table dropdown")
	public ResponseEntity<Response<String>> listSourceTables() {
		return respond(getSuccessResponse("Found", archiveConfigService.listSourceTables()));
	}

	@GetMapping("/schemas")
	@Operation(summary = "List DB schemas, for the archive schema dropdown")
	public ResponseEntity<Response<String>> listArchiveSchemas() {
		return respond(getSuccessResponse("Found", archiveConfigService.listArchiveSchemas()));
	}

	@GetMapping("/date-columns")
	@Operation(summary = "List a source table's date/timestamp columns, for the date column dropdown")
	public ResponseEntity<Response<String>> listDateColumns(@RequestParam String sourceTable) {
		return respond(getSuccessResponse("Found", archiveConfigService.listDateColumns(sourceTable)));
	}

	@PostMapping("/{id}/archive-now")
	@Operation(summary = "Run this config's archive job immediately, outside its schedule")
	public ResponseEntity<Response<Integer>> archiveNow(@PathVariable Long id) {
		return respond(archiveConfigService.archiveNow(id));
	}

	@GetMapping("/{id}/archived-data")
	@Operation(summary = "Browse this config's archive table")
	public ResponseEntity<Response<ArchivedDataResDTO>> findArchivedData(
			@PathVariable Long id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		return respond(archiveConfigService.findArchivedData(id, page, size));
	}
}
