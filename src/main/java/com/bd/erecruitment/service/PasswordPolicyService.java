package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.PasswordPolicyReqDto;
import com.bd.erecruitment.dto.res.PasswordPolicyResDTO;
import com.bd.erecruitment.util.Response;

public interface PasswordPolicyService {

	Response<PasswordPolicyResDTO> find();

	Response<PasswordPolicyResDTO> update(PasswordPolicyReqDto reqDto);

	void validatePassword(String password, String email, String fullName);
}
