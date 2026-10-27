package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.LogoImageReqDto;
import com.bd.erecruitment.dto.req.OrganizationReqDto;
import com.bd.erecruitment.dto.res.OrganizationResDTO;
import com.bd.erecruitment.service.impl.OrganizationServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/organization")
@Tag(name = "3.1 Organization", description = "API")
public class OrganizationController extends AbstractBaseController<OrganizationResDTO, OrganizationReqDto> {

	private final OrganizationServiceImpl organizationService;

	public OrganizationController(OrganizationServiceImpl organizationService) {
		super(organizationService);
		this.organizationService = organizationService;
	}

	@Operation(summary = "Upload or remove the organization logo")
	@PutMapping("/{id}/logo")
	public Response<OrganizationResDTO> updateLogo(@PathVariable Long id, @RequestBody(required = false) LogoImageReqDto reqDto) {
		return organizationService.updateLogo(id, reqDto);
	}
}
