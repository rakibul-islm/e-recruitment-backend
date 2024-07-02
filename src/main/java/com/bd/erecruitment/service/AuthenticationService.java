package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.AuthenticationReqDTO;
import com.bd.erecruitment.dto.req.TokenValidationReqDTO;
import com.bd.erecruitment.dto.res.AuthenticationResDTO;
import com.bd.erecruitment.dto.res.TokenValidationResDTO;
import com.bd.erecruitment.util.Response;

public interface AuthenticationService <R, E> extends BaseService<R, E> {

	public Response<AuthenticationResDTO> generateToken(AuthenticationReqDTO reqDto);
	public Response<TokenValidationResDTO> validateToken(TokenValidationReqDTO reqDto);
}
