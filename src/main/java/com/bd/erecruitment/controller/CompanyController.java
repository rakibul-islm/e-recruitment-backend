package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.CompanyReqDto;
import com.bd.erecruitment.dto.res.CompanyResDTO;
import com.bd.erecruitment.service.BaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/company")
@Tag(name = "3.1 Company", description = "API")
public class CompanyController extends AbstractBaseController<CompanyResDTO, CompanyReqDto> {

	public CompanyController(BaseService<CompanyResDTO, CompanyReqDto> service) {
		super(service);
	}
}
