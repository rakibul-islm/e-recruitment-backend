package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.OrganizationTypeReqDto;
import com.bd.erecruitment.dto.res.OrganizationTypeResDTO;
import com.bd.erecruitment.service.BaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/organization-type")
@Tag(name = "3.1a Organization Type", description = "Managed lookup list for Organization.organizationType")
public class OrganizationTypeController extends AbstractBaseController<OrganizationTypeResDTO, OrganizationTypeReqDto> {

	public OrganizationTypeController(BaseService<OrganizationTypeResDTO, OrganizationTypeReqDto> service) {
		super(service);
	}
}
