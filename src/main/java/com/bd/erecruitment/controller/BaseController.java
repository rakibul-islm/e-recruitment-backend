package com.bd.erecruitment.controller;

import com.bd.erecruitment.util.Response;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface BaseController<R, E> {

	public Response<R> getAll();

	public Response<R> save(@RequestBody E e);

	public Response<R> update(@RequestBody E e);

	public Response<R> find(@PathVariable Long id);

//	public Response<R> delete(@RequestBody E e);

	public Response<R> delete(@PathVariable Long id);
}
