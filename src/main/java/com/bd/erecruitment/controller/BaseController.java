package com.bd.erecruitment.controller;

import com.bd.erecruitment.util.Response;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface BaseController<R, E> {

	ResponseEntity<Response<R>> filter(@RequestParam Map<String, String> filters, Pageable pageable, Boolean isPageable);

	ResponseEntity<Response<R>> save(@RequestBody E e);

	ResponseEntity<Response<R>> update(@RequestBody E e);

	ResponseEntity<Response<R>> find(@PathVariable Long id);

	ResponseEntity<Response<R>> delete(@PathVariable Long id);

	ResponseEntity<Response<R>> remove(@PathVariable Long id);
}
