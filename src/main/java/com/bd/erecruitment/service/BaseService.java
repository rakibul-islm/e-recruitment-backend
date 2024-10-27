package com.bd.erecruitment.service;

import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;
import org.springframework.data.domain.Pageable;

public interface BaseService<R, E> {

	public Response<R> find(Long id) throws ServiceException;

	public Response<R> save(E reqDto) throws ServiceException;

	public Response<R> update(E reqDto) throws ServiceException;

	public Response<R> getAll(Pageable pageable, Boolean isPageable) throws ServiceException;

	public Response<R> delete(E reqDto) throws ServiceException;

	public Response<R> remove(Long id) throws ServiceException;
}
