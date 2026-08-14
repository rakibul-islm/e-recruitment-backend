package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.PasswordPolicyReqDto;
import com.bd.erecruitment.dto.res.PasswordPolicyResDTO;
import com.bd.erecruitment.service.PasswordPolicyService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/password-policy")
@Tag(name = "Password Policy")
@RequiredArgsConstructor
public class PasswordPolicyController implements ResponseWrapper {

	private final PasswordPolicyService service;

	@GetMapping
	@Operation(summary = "Get current password policy")
	public ResponseEntity<Response<PasswordPolicyResDTO>> find() {
		return respond(service.find());
	}

	@PutMapping
	@Operation(summary = "Update password policy")
	public ResponseEntity<Response<PasswordPolicyResDTO>> update(@RequestBody PasswordPolicyReqDto reqDto) {
		return respond(service.update(reqDto));
	}
}
