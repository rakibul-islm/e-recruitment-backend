package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.UserSignupReqDto;
import com.bd.erecruitment.dto.res.UserProfileResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;

public interface UserService<R, E> extends BaseService<R, E>{
	
	public Response<UserResDTO> saveNormalUser(UserSignupReqDto reqDto) throws ServiceException;
	
	public Response<UserProfileResDTO> userProfile() throws ServiceException;
}
