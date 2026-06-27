package com.bd.erecruitment.exception;

public class BadRequestException extends ApiException {
	public BadRequestException(String message) { super(400, message); }
}
