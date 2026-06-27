package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.exception.UnauthorizedException;
import com.bd.erecruitment.util.Response;
import org.springframework.data.domain.Page;

import java.util.List;

public class CommonFunctionsImpl {

	// ── success responses (return a value) ──────────────────────────────────

	protected <R> Response<R> getSuccessResponse(String message) {
		return build(200, true, message, null, null, null);
	}

	protected <R> Response<R> getSuccessResponse(String message, R obj) {
		return build(200, true, message, obj, null, null);
	}

	protected <R> Response<R> getSuccessResponse(String message, List<R> list) {
		return build(200, true, message, null, list, null);
	}

	protected <R> Response<R> getSuccessResponse(String message, Page<R> page) {
		return build(200, true, message, null, null, page);
	}

	protected <R> Response<R> getCreatedResponse(String message, R obj) {
		return build(201, true, message, obj, null, null);
	}

	// ── error helpers (throw, no return needed) ─────────────────────────────

	protected void returnErrorException(String message) {
		throw new BadRequestException(message);
	}

	protected void returnUnauthorizedException(String message) {
		throw new UnauthorizedException(message);
	}

	protected void returnForbiddenException(String message) {
		throw new ForbiddenException(message);
	}

	protected void returnNotFoundException(String message) {
		throw new NotFoundException(message);
	}

	// ── internal builder ────────────────────────────────────────────────────

	private <R> Response<R> build(int code, boolean success, String message, R obj, List<R> list, Page<R> page) {
		Response<R> res = new Response<>();
		res.setCode(code);
		res.setSuccess(success);
		res.setMessage(message);
		res.setObj(obj);
		res.setList(list);
		res.setPage(page);
		return res;
	}
}
