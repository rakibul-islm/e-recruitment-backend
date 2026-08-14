package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.ExceptionLogReqDto;
import com.bd.erecruitment.dto.res.ExceptionLogResDTO;
import com.bd.erecruitment.service.impl.ExceptionLogServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/exception-log")
@Tag(name = "Exception Log")
public class ExceptionLogController extends AbstractBaseController<ExceptionLogResDTO, ExceptionLogReqDto> {

	ExceptionLogController(ExceptionLogServiceImpl service) {
		super(service);
	}

	@Hidden @Override public ResponseEntity<Response<ExceptionLogResDTO>> save(ExceptionLogReqDto e) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<ExceptionLogResDTO>> update(ExceptionLogReqDto e) { return ResponseEntity.status(501).build(); }
}
