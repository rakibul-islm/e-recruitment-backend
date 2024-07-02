package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.service.CommonFunctions;
import com.bd.erecruitment.util.Response;
import org.springframework.beans.BeanUtils;
import java.util.Collections;
import java.util.List;

public class CommonFunctionsImpl<R> implements CommonFunctions<R> {

	@Override
	public Response<R> getSuccessResponse(String message) {
		return getSuccessResponse(null, message);
	}

	@Override
	public Response<R> getSuccessResponse(String code, String message) {
		Response<R> response = new Response<R>();
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		return response;
	}

	@Override
	public Response<R> getSuccessResponse(String message, R r) {
		return getSuccessResponse(null, message, r);
	}

	@Override
	public Response<R> getSuccessResponse(String code, String message, R r) {
		Response<R> response = new Response<R>();
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		response.setObj(r);
		return response;
	}

	@Override
	public Response<R> getSuccessResponse(String message, List<R> list) {
		return getSuccessResponse(null, message, list);
	}

	@Override
	public Response<R> getSuccessResponse(String code, String message, List<R> list) {
		Response<R> response = new Response<R>();
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		response.setItems(list);
		return response;
	}

	@Override
	public Response<R> getSuccessResponse(String message, Response<R> response) {
		return getSuccessResponse(null, message, response);
	}

	@Override
	public Response<R> getSuccessResponse(String code, String message, Response<R> response) {
		response.setSuccess(true);
		response.setCode(code);
		response.setMessage(message);
		return response;
	}

	@Override
	public Response<R> getErrorResponse(String message) {
		Response<R> response = new Response<R>();
		response.setSuccess(false);
		response.setMessage(message);
		response.setItems(Collections.emptyList());
		response.setObj(null);
		response.setModel(Collections.emptyMap());
		response.setPage(null);
		return response;
	}

	@Override
	public Response<R> getErrorResponse(String code, String message) {
		Response<R> response = new Response<R>();
		response.setSuccess(false);
		response.setCode(code);
		response.setMessage(message);
		response.setItems(Collections.emptyList());
		response.setObj(null);
		response.setModel(Collections.emptyMap());
		response.setPage(null);
		return response;
	}

	@Override
	public Response<R> getErrorResponse(String code, String message, Response<R> response) {
		response.setSuccess(false);
		response.setCode(code);
		response.setMessage(message);
		if(response.getItems() == null || response.getItems().isEmpty()) response.setItems(Collections.emptyList());
		if(response.getObj() == null) response.setObj(null);
		if(response.getModel() == null || response.getModel().isEmpty()) response.setModel(Collections.emptyMap());
		if(response.getPage() == null) response.setPage(null);
		return response;
	}

	@Override
	public UserResDTO getSystemAdminUser() {
		UserResDTO responseDto = new UserResDTO();
		User u = new User();

		BeanUtils.copyProperties(u, responseDto);
		return responseDto;
	}

}