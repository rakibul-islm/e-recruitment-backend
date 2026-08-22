package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.AuditLogReqDto;
import com.bd.erecruitment.dto.res.AuditLogResDTO;
import com.bd.erecruitment.service.impl.AuditLogServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/audit-log")
@Tag(name = "Audit Log")
public class AuditLogController extends AbstractBaseController<AuditLogResDTO, AuditLogReqDto> {

	AuditLogController(AuditLogServiceImpl service) {
		super(service);
	}

	@Hidden @Override public ResponseEntity<Response<AuditLogResDTO>> save(AuditLogReqDto e) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuditLogResDTO>> update(AuditLogReqDto e) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuditLogResDTO>> delete(Long id) { return ResponseEntity.status(501).build(); }
	@Hidden @Override public ResponseEntity<Response<AuditLogResDTO>> remove(Long id) { return ResponseEntity.status(501).build(); }
}
