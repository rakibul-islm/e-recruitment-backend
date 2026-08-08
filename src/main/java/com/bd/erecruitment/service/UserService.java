package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.req.UserSignupReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.util.Response;

public interface UserService<R, E> extends BaseService<R, E> {

	Response<UserResDTO> saveNormalUser(UserSignupReqDto reqDto);

	Response<UserProfileResDTO> userProfile();

	Response<UserResDTO> updateProfile(UserReqDto reqDto);
}
