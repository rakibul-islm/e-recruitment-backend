package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.AuthenticationReqDTO;
import com.bd.erecruitment.dto.req.TokenValidationReqDTO;
import com.bd.erecruitment.dto.res.AuthenticationResDTO;
import com.bd.erecruitment.dto.res.TokenValidationResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.service.AuthenticationService;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/e-recruitment/authenticate")
@Tag(name = "1.0 Authentication", description = "API")
public class AuthenticationController extends AbstractBaseController<User, AuthenticationResDTO, AuthenticationReqDTO> {

	private AuthenticationService<AuthenticationResDTO, AuthenticationReqDTO> authservice;

	public AuthenticationController(AuthenticationService<AuthenticationResDTO, AuthenticationReqDTO> authservice) {
		super(authservice);
		this.authservice = authservice;
	}

	@Operation(summary = "Generate access token")
	@PostMapping("/token")
	public Response<AuthenticationResDTO> generateToken(@RequestBody AuthenticationReqDTO reqDto) throws Exception {
		return authservice.generateToken(reqDto);
	}

	@Operation(summary = "Validate Token")
	@PostMapping("/token/validate")
	public Response<TokenValidationResDTO> validateToken(@RequestBody TokenValidationReqDTO reqDto) throws Exception {
		return authservice.validateToken(reqDto);
	}

	@Hidden
	@Override
	public Response<AuthenticationResDTO> getAll() {
		// TODO Auto-generated method stub
		return super.getAll();
	}

	@Hidden
	@Override
	public Response<AuthenticationResDTO> save(AuthenticationReqDTO e) {
		// TODO Auto-generated method stub
		return super.save(e);
	}

	@Hidden
	@Override
	public Response<AuthenticationResDTO> update(AuthenticationReqDTO e) {
		// TODO Auto-generated method stub
		return super.update(e);
	}

	@Hidden
	@Override
	public Response<AuthenticationResDTO> find(Long id) {
		// TODO Auto-generated method stub
		return super.find(id);
	}

//	@ApiIgnore
//	@Override
//	public Response<AuthenticationResDTO> delete(AuthenticationReqDTO e) {
//		// TODO Auto-generated method stub
//		return super.delete(e);
//	}

	@Hidden
	@Override
	public Response<AuthenticationResDTO> delete(Long id) {
		// TODO Auto-generated method stub
		return super.delete(id);
	}

}
