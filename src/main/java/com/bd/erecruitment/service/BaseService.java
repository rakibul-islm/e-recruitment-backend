package com.bd.erecruitment.service;

import com.bd.erecruitment.util.Response;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface BaseService<R, E> {

	Response<R> find(Long id);

	Response<R> save(E reqDto);

	Response<R> update(E reqDto);

	Response<R> delete(Long id);

	Response<R> remove(Long id);

	default Response<R> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) { return null; }
}
