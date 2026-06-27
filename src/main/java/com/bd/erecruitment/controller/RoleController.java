package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.RoleReqDto;
import com.bd.erecruitment.dto.res.RoleResDTO;
import com.bd.erecruitment.service.impl.RoleServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/role")
@Tag(name = "Role")
public class RoleController extends AbstractBaseController<RoleResDTO, RoleReqDto> {

	RoleController(RoleServiceImpl service) {
		super(service);
	}
}
