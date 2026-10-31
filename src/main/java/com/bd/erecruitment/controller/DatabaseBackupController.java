package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.res.DatabaseBackupResDTO;
import com.bd.erecruitment.service.impl.DatabaseBackupServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestApiController
@RequestMapping("/backup")
@RequiredArgsConstructor
@Tag(name = "Database Backup", description = "Create, list, download and delete logical database backups")
public class DatabaseBackupController implements ResponseWrapper {

	private final DatabaseBackupServiceImpl backupService;

	@GetMapping
	@Operation(summary = "List backups, newest first")
	public ResponseEntity<Response<DatabaseBackupResDTO>> list() {
		return respond(backupService.list());
	}

	@PostMapping
	@Operation(summary = "Start a backup in the background")
	public ResponseEntity<Response<DatabaseBackupResDTO>> create() {
		return respond(backupService.create());
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "Upload a backup archive made elsewhere so it can be restored")
	public ResponseEntity<Response<DatabaseBackupResDTO>> upload(@RequestParam("file") MultipartFile file) {
		return respond(backupService.upload(file));
	}

	@PostMapping("/{id}/drive-upload")
	@Operation(summary = "Upload (or retry uploading) a completed backup to Google Drive")
	public ResponseEntity<Response<DatabaseBackupResDTO>> uploadToDrive(@PathVariable Long id) {
		return respond(backupService.uploadToDriveNow(id));
	}

	@GetMapping("/{id}/download")
	@Operation(summary = "Download a completed backup archive")
	public ResponseEntity<Resource> download(@PathVariable Long id) {
		DatabaseBackupServiceImpl.BackupFile file = backupService.download(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType("application/zip"))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
			.body(new FileSystemResource(file.path()));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a backup and its file")
	public ResponseEntity<Response<DatabaseBackupResDTO>> delete(@PathVariable Long id) {
		return respond(backupService.delete(id));
	}
}
