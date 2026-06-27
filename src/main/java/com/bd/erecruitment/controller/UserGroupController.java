package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.UserGroupReqDto;
import com.bd.erecruitment.dto.res.UserGroupResDTO;
import com.bd.erecruitment.service.impl.UserGroupServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/user-group")
@Tag(name = "User Group")
public class UserGroupController extends AbstractBaseController<UserGroupResDTO, UserGroupReqDto> {

	UserGroupController(UserGroupServiceImpl service) {
		super(service);
	}
}
