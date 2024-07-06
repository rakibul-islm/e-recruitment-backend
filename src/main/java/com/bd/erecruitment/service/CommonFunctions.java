package com.bd.erecruitment.service;

import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.util.Response;

import java.util.List;

public interface CommonFunctions<R> {
	Response<R> getSuccessResponse(String message);

	Response<R> getSuccessResponse(String code, String message);

	Response<R> getSuccessResponse(String message, R r);

	Response<R> getSuccessResponse(String code, String message, R r);

	Response<R> getSuccessResponse(String message, List<R> list);

	Response<R> getSuccessResponse(String code, String message, List<R> list);

	Response<R> getSuccessResponse(String message, Response<R> response);

	Response<R> getSuccessResponse(String code, String message, Response<R> response);

	Response<R> getErrorResponse(String message);

	Response<R> getErrorResponse(String code, String message);

	Response<R> getErrorResponse(String code, String message, Response<R> response);

}
