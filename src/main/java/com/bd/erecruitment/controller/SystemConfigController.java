package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.SystemConfigReqDto;
import com.bd.erecruitment.dto.res.SystemConfigResDTO;
import com.bd.erecruitment.service.impl.SystemConfigServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/system-config")
@Tag(name = "System Config")
public class SystemConfigController extends AbstractBaseController<SystemConfigResDTO, SystemConfigReqDto> {

	SystemConfigController(SystemConfigServiceImpl service) {
		super(service);
	}
}
