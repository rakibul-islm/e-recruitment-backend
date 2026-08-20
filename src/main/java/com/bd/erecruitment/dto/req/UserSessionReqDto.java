package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.UserSession;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserSessionReqDto extends BaseRequestDTO<UserSession> {

	@JsonIgnore
	@Override
	public UserSession getBean() {
		throw new UnsupportedOperationException("Sessions cannot be created or updated directly");
	}
}
