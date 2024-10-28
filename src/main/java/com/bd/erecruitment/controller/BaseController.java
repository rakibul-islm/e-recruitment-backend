package com.bd.erecruitment.controller;

import com.bd.erecruitment.util.Response;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface BaseController<R, E> {

	public Response<R> getAll(@Nullable Pageable pageable, Boolean isPageable);

	public Response<R> save(@RequestBody E e);

	public Response<R> update(@RequestBody E e);

	public Response<R> find(@PathVariable Long id);

	public Response<R> delete(@RequestBody E e);

	public Response<R> remove(@PathVariable Long id);
}
