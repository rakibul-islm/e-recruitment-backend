package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.CompanyTypeReqDto;
import com.bd.erecruitment.dto.res.CompanyTypeResDTO;
import com.bd.erecruitment.service.BaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/company-type")
@Tag(name = "3.1a Company Type", description = "Managed lookup list for Company.industry")
public class CompanyTypeController extends AbstractBaseController<CompanyTypeResDTO, CompanyTypeReqDto> {

	public CompanyTypeController(BaseService<CompanyTypeResDTO, CompanyTypeReqDto> service) {
		super(service);
	}
}
