package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.ChangePasswordReqDto;
import com.bd.erecruitment.dto.req.RequestChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.req.ResendSignupOtpReqDto;
import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.UserSignupReqDto;
import com.bd.erecruitment.dto.req.VerifyChangePasswordOtpReqDto;
import com.bd.erecruitment.dto.req.VerifySignupOtpReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.util.Response;

public interface UserService<R, E> extends BaseService<R, E> {

	Response<UserResDTO> saveNormalUser(UserSignupReqDto reqDto);

	Response<Object> verifySignupOtp(VerifySignupOtpReqDto reqDto);

	Response<Object> resendSignupOtp(ResendSignupOtpReqDto reqDto);

	Response<UserProfileResDTO> userProfile();

	Response<UserResDTO> updateProfile(UserReqDto reqDto);

	Response<Object> requestChangePasswordOtp(RequestChangePasswordOtpReqDto reqDto);

	Response<Object> verifyChangePasswordOtp(VerifyChangePasswordOtpReqDto reqDto);

	Response<Object> changePassword(ChangePasswordReqDto reqDto);
}
