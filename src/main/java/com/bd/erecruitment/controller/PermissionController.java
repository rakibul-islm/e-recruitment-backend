package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.PermissionReqDto;
import com.bd.erecruitment.dto.res.PermissionResDTO;
import com.bd.erecruitment.service.impl.PermissionServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/permission")
@Tag(name = "Permission")
public class PermissionController extends AbstractBaseController<PermissionResDTO, PermissionReqDto> {

	PermissionController(PermissionServiceImpl service) {
		super(service);
	}
}
