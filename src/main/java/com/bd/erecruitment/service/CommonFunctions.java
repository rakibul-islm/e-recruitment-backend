package com.bd.erecruitment.service;

import com.bd.erecruitment.util.Response;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CommonFunctions {
	<R>Response<R> getSuccessResponse(String message);

	<R>Response<R> getSuccessResponse(String message, R r);

	<R>Response<R> getSuccessResponse(String message, List<R> list);

	<R>Response<R> getSuccessResponse(String message, Page<R> page);

	<R>Response<R> getErrorResponse(String message);
}
