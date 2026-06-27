package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.AuthenticationReqDTO;
import com.bd.erecruitment.dto.req.GoogleAuthReqDTO;
import com.bd.erecruitment.dto.res.AuthenticationResDTO;
import com.bd.erecruitment.util.Response;

public interface AuthenticationService<R, E> extends BaseService<R, E> {

	Response<AuthenticationResDTO> generateToken(AuthenticationReqDTO reqDto);
	Response<AuthenticationResDTO> loginWithGoogle(GoogleAuthReqDTO reqDto);
}
