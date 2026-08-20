package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.req.UserSessionReqDto;
import com.bd.erecruitment.dto.res.SessionSummaryResDTO;
import com.bd.erecruitment.dto.res.UserSessionResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.entity.UserSession;
import com.bd.erecruitment.util.Response;

import java.util.Date;

public interface UserSessionService extends BaseService<UserSessionResDTO, UserSessionReqDto> {

	UserSession createSession(User user, String jti, Date issuedAt, Date expiresAt);

	boolean isActive(String jti);

	Response<Object> forceLogoutUser(Long userId);

	Response<Object> forceLogoutAll();

	Response<Object> logoutCurrentSession(String jti);

	Response<UserSessionResDTO> findByUser(Long userId);

	Response<SessionSummaryResDTO> getSummary();
}
