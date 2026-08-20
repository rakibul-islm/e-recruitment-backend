package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.AuthenticationReqDTO;
import com.bd.erecruitment.dto.req.ForgotPasswordReqDto;
import com.bd.erecruitment.dto.req.GoogleAuthReqDTO;
import com.bd.erecruitment.dto.req.ResetPasswordReqDto;
import com.bd.erecruitment.dto.req.SetPasswordReqDto;
import com.bd.erecruitment.dto.req.VerifyOtpReqDto;
import com.bd.erecruitment.dto.res.AuthenticationResDTO;
import com.bd.erecruitment.util.Response;

public interface AuthenticationService<R, E> extends BaseService<R, E> {

	Response<AuthenticationResDTO> generateToken(AuthenticationReqDTO reqDto);
	Response<AuthenticationResDTO> loginWithGoogle(GoogleAuthReqDTO reqDto);
	Response<Object> forgotPassword(ForgotPasswordReqDto reqDto);
	Response<Object> verifyOtp(VerifyOtpReqDto reqDto);
	Response<Object> resetPassword(ResetPasswordReqDto reqDto);
	Response<Object> setPassword(SetPasswordReqDto reqDto);
	Response<Object> logout(String authorizationHeader);
}
