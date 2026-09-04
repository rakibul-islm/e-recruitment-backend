package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.CreateOfferReqDto;
import com.bd.erecruitment.dto.req.OfferResponseReqDto;
import com.bd.erecruitment.dto.res.OfferResDTO;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.service.impl.OfferServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestApiController
@RequestMapping("/offer")
@RequiredArgsConstructor
@Tag(name = "5.1 Offer", description = "Offer letter generation and candidate response")
public class OfferController {

	private final OfferServiceImpl offerService;

	@Operation(summary = "Create an offer (recruiter/admin)")
	@PostMapping
	public Response<OfferResDTO> create(@RequestBody CreateOfferReqDto reqDto) {
		return offerService.createOffer(reqDto);
	}

	@Operation(summary = "Generate the offer letter PDF (recruiter/admin)")
	@PostMapping("/{id}/generate-letter")
	public Response<OfferResDTO> generateLetter(@PathVariable Long id) {
		return offerService.generateLetter(id);
	}

	@Operation(summary = "Send the offer to the candidate (recruiter/admin)")
	@PutMapping("/{id}/send")
	public Response<OfferResDTO> send(@PathVariable Long id) {
		return offerService.send(id);
	}

	@Operation(summary = "Find offer by id")
	@GetMapping("/{id}")
	public Response<OfferResDTO> find(@PathVariable Long id) {
		return offerService.find(id);
	}

	@Operation(summary = "Offers for an application (owner or recruiter/admin)")
	@GetMapping("/by-application/{applicationId}")
	public Response<OfferResDTO> findByApplication(@PathVariable Long applicationId) {
		return offerService.findByApplication(applicationId);
	}

	@Operation(summary = "My offers")
	@GetMapping("/my")
	public Response<OfferResDTO> myOffers() {
		return offerService.myOffers();
	}

	@Operation(summary = "Accept or decline an offer (candidate)")
	@PutMapping("/{id}/respond")
	public Response<OfferResDTO> respond(@PathVariable Long id, @RequestBody OfferResponseReqDto reqDto) {
		return offerService.respond(id, reqDto);
	}

	@Operation(summary = "Download the offer letter PDF")
	@GetMapping("/{id}/letter")
	public ResponseEntity<byte[]> downloadLetter(@PathVariable Long id) {
		StoredFile file = offerService.downloadLetter(id);
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(file.getContentType()))
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
			.body(file.getData());
	}
}
