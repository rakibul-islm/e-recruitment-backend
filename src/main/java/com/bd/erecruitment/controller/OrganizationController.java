package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.OrganizationReqDto;
import com.bd.erecruitment.dto.res.OrganizationResDTO;
import com.bd.erecruitment.service.BaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/organization")
@Tag(name = "3.1 Organization", description = "API")
public class OrganizationController extends AbstractBaseController<OrganizationResDTO, OrganizationReqDto> {

	public OrganizationController(BaseService<OrganizationResDTO, OrganizationReqDto> service) {
		super(service);
	}
}
