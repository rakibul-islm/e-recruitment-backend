package com.bd.erecruitment.controller;

import com.bd.erecruitment.util.Response;
import org.springframework.http.ResponseEntity;

public interface ResponseWrapper {

	default <R> ResponseEntity<Response<R>> respond(Response<R> result) {
		return ResponseEntity.status(result.getCode()).body(result);
	}
}
