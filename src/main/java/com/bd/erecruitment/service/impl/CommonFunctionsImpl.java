package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.exception.UnauthorizedException;
import com.bd.erecruitment.util.Response;
import com.bd.erecruitment.model.MyUserDetail;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public class CommonFunctionsImpl {

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

	protected void returnErrorException(String message) {
		throw new BadRequestException(message);
	}

	protected void returnUnauthorizedException(String message) {
		throw new UnauthorizedException(message);
	}

	protected void returnNotFoundException(String message) {
		throw new NotFoundException(message);
	}

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

	protected MyUserDetail getLoggedInUserDetails() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return null;
		Object principal = auth.getPrincipal();
		return principal instanceof MyUserDetail mud ? mud : null;
	}

	private static final java.util.Set<String> UNRESTRICTED_ROLE_CODES = java.util.Set.of("MANAGER", "EDITOR", "VIEWER");

	protected boolean isScopedRecruiter() {
		MyUserDetail me = getLoggedInUserDetails();
		if (me == null) return false;
		if (me.getAuthorities().stream().anyMatch(a -> "SUPER_ADMIN".equals(a.getAuthority()))) return false;
		if (me.getRoleCodes().stream().anyMatch(UNRESTRICTED_ROLE_CODES::contains)) return false;
		return me.getRoleCodes().contains("RECRUITER");
	}
}
