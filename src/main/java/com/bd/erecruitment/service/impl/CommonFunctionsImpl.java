package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.service.CommonFunctions;
import com.bd.erecruitment.util.Response;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

public class CommonFunctionsImpl implements CommonFunctions {

	@Override
	public <R>Response<R> getSuccessResponse(String message) {
		return getSuccessResponse(null, message);
	}

	@Override
	public <R>Response<R> getSuccessResponse(String code, String message) {
		Response<R> response = new Response<>();
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		return response;
	}

	@Override
	public <R>Response<R> getSuccessResponse(String message, R r) {
		return getSuccessResponse(null, message, r);
	}

	@Override
	public <R>Response<R> getSuccessResponse(String code, String message, R r) {
		Response<R> response = new Response<>();
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		response.setObj(r);
		return response;
	}

	@Override
	public <R>Response<R> getSuccessResponse(String message, List<R> list) {
		return getSuccessResponse(null, message, list);
	}

	@Override
	public <R>Response<R> getSuccessResponse(String code, String message, List<R> list) {
		Response<R> response = new Response<>();
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		response.setList(list);
		return response;
	}

	@Override
	public <R>Response<R> getSuccessResponse(String message, Page<R> page) {
		Response<R> response = new Response<>();
		response.setSuccess(true);
		response.setMessage(message);
		response.setPage(page);
		return response;
	}

	@Override
	public <R>Response<R> getSuccessResponse(String message, Response<R> response) {
		return getSuccessResponse(null, message, response);
	}

	@Override
	public <R>Response<R> getSuccessResponse(String code, String message, Response<R> response) {
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		return response;
	}

	@Override
	public <R>Response<R> getErrorResponse(String message) {
		Response<R> response = new Response<>();
		response.setSuccess(false);
		response.setMessage(message);
		response.setList(Collections.emptyList());
		response.setObj(null);
		response.setModel(Collections.emptyMap());
		response.setPage(null);
		return response;
	}

	@Override
	public <R>Response<R> getErrorResponse(String code, String message) {
		Response<R> response = new Response<>();
		response.setSuccess(false);
		response.setCode(code);
		response.setMessage(message);
		response.setList(Collections.emptyList());
		response.setObj(null);
		response.setModel(Collections.emptyMap());
		response.setPage(null);
		return response;
	}

	@Override
	public <R>Response<R> getErrorResponse(String code, String message, Response<R> response) {
		response.setSuccess(false);
		response.setCode(code);
		response.setMessage(message);
		if(response.getList() == null || response.getList().isEmpty()) response.setList(Collections.emptyList());
		if(response.getObj() == null) response.setObj(null);
		if(response.getModel() == null || response.getModel().isEmpty()) response.setModel(Collections.emptyMap());
		if(response.getPage() == null) response.setPage(null);
		return response;
	}
}
