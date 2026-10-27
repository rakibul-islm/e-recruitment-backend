package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.repository.BannerRepo;
import com.bd.erecruitment.repository.OrganizationRepo;
import com.bd.erecruitment.service.impl.BannerServiceImpl;
import com.bd.erecruitment.service.impl.OrganizationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "3.2 Public Assets", description = "Anonymous, cacheable image serving (logos, banners)")
public class PublicAssetController {

	// Immutable by fileId: every upload creates a new StoredFile.
	private static final String ASSET_CACHE_CONTROL = "public, max-age=31536000, immutable";

	private final OrganizationServiceImpl organizationService;
	private final BannerServiceImpl bannerService;
	private final OrganizationRepo organizationRepo;
	private final BannerRepo bannerRepo;

	@Operation(summary = "Organization logo by file id (public, immutable-cached)")
	@GetMapping("/logo/{fileId}")
	public ResponseEntity<byte[]> logo(
			@PathVariable Long fileId,
			@RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
		StoredFile file = organizationService.publicLogo(fileId);
		return serve(file, ifNoneMatch);
	}

	@Operation(summary = "Dashboard banner image by file id (public, immutable-cached)")
	@GetMapping("/banner/{fileId}")
	public ResponseEntity<byte[]> banner(
			@PathVariable Long fileId,
			@RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
		if (fileId == null || !bannerRepo.existsByFileIdAndDeletedFalse(fileId)) {
			return ResponseEntity.notFound().build();
		}
		return serve(bannerService.publicImage(fileId), ifNoneMatch);
	}

	private ResponseEntity<byte[]> serve(StoredFile file, String ifNoneMatch) {
		String etag = "\"" + file.getId() + "-" + file.getSize() + "\"";
		if (etag.equals(ifNoneMatch)) {
			return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
				.header(HttpHeaders.CACHE_CONTROL, ASSET_CACHE_CONTROL)
				.header(HttpHeaders.ETAG, etag)
				.build();
		}
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.getContentType()))
			.header(HttpHeaders.CACHE_CONTROL, ASSET_CACHE_CONTROL)
			.header(HttpHeaders.ETAG, etag)
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline")
			.body(file.getData());
	}
}
