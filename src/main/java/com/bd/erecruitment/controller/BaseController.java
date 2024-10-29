package com.bd.erecruitment.controller;

import com.bd.erecruitment.util.Response;
import jakarta.annotation.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public interface BaseController<R, E> {

	Response<R> getAll(@Nullable Pageable pageable, Boolean isPageable);

	Response<R> save(@RequestBody E e);

	Response<R> update(@RequestBody E e);

	Response<R> find(@PathVariable Long id);

	Response<R> delete(@RequestBody E e);

	Response<R> remove(@PathVariable Long id);
}
