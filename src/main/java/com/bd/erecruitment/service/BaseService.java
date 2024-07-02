package com.bd.erecruitment.service;

import com.bd.erecruitment.service.exception.ServiceException;
import com.bd.erecruitment.util.Response;

public interface BaseService<R, E> {

	public Response<R> find(Long id) throws ServiceException;

	public Response<R> save(E reqDto) throws ServiceException;

	public Response<R> update(E reqDto) throws ServiceException;

	public Response<R> getAll() throws ServiceException;

	public Response<R> delete(E reqDto) throws ServiceException;

	public Response<R> delete(Long id) throws ServiceException;
}
