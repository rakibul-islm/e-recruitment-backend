package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.SystemConfigReqDto;
import com.bd.erecruitment.dto.res.SystemConfigResDTO;
import com.bd.erecruitment.service.impl.SystemConfigServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/system-config")
@Tag(name = "System Config")
public class SystemConfigController extends AbstractBaseController<SystemConfigResDTO, SystemConfigReqDto> {

	private final SystemConfigServiceImpl systemConfigService;

	SystemConfigController(SystemConfigServiceImpl service) {
		super(service);
		this.systemConfigService = service;
	}

	@Hidden @PostMapping @Override public ResponseEntity<Response<SystemConfigResDTO>> save(@RequestBody SystemConfigReqDto e) { return super.save(e); }
	@Hidden @DeleteMapping("/delete/{id}") @Override public ResponseEntity<Response<SystemConfigResDTO>> delete(@PathVariable Long id) { return super.delete(id); }
	@Hidden @DeleteMapping("/{id}") @Override public ResponseEntity<Response<SystemConfigResDTO>> remove(@PathVariable Long id) { return super.remove(id); }

	@GetMapping("/key/{configKey}")
	@Operation(summary = "Find by config key")
	public ResponseEntity<Response<SystemConfigResDTO>> findByKey(@PathVariable String configKey) {
		return respond(systemConfigService.findByKey(configKey));
	}
}
