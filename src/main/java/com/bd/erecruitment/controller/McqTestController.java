package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.McqTestReqDto;
import com.bd.erecruitment.dto.res.McqTestResDTO;
import com.bd.erecruitment.service.BaseService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/mcq-test")
@Tag(name = "5.2 MCQ Test Builder", description = "Tests composed from the MCQ question bank")
public class McqTestController extends AbstractBaseController<McqTestResDTO, McqTestReqDto> {

	public McqTestController(BaseService<McqTestResDTO, McqTestReqDto> service) {
		super(service);
	}
}
