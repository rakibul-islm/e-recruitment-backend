package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.service.CommonFunctions;
import com.bd.erecruitment.util.Response;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CommonFunctionsImpl implements CommonFunctions {

	private <R> Response<R> createResponse(boolean success, String message, R obj, List<R> list, Page<R> page, Map<String, R> model) {
		Response<R> response = new Response<>();
		response.setSuccess(success);
		response.setMessage(message);
		response.setObj(obj);
		response.setList(list != null ? list : Collections.emptyList());
		response.setPage(page);
		response.setModel(model != null ? model : Collections.emptyMap());
		return response;
	}

	@Override
	public <R> Response<R> getSuccessResponse(String message) {
		return createResponse(true, message, null, null, null, null);
	}

	@Override
	public <R> Response<R> getSuccessResponse(String message, R obj) {
		return createResponse(true, message, obj, null, null, null);
	}

	@Override
	public <R> Response<R> getSuccessResponse(String message, List<R> list) {
		return createResponse(true, message, null, list, null, null);
	}

	@Override
	public <R> Response<R> getSuccessResponse(String message, Page<R> page) {
		return createResponse(true, message, null, null, page, null);
	}

	@Override
	public <R> Response<R> getErrorResponse(String message) {
		return createResponse(false, message, null, null, null, null);
	}
}
