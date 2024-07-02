package com.bd.erecruitment.controller;

import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.impl.CommonFunctionsImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
public class AbstractBaseController<P, R, E> extends CommonFunctionsImpl<R> implements BaseController<R, E> {

	protected final BaseService<R, E> service;

	@GetMapping
	@Operation(summary = "Get All")
	@Override
	public Response<R> getAll() {
		try {
			return service.getAll();
		} catch (Exception e) {
			log.error("Error is {}, {}", e.getMessage(), e);
			return getErrorResponse(e.getMessage());
		}
	}

	@PostMapping
	@Operation(summary  = "Save")
	@Override
	public Response<R> save(@RequestBody E e) {
		try {
			return service.save(e);
		} catch (Exception e1) {
			log.error("Error is {}, {}", e1.getMessage(), e1);
			return getErrorResponse(e1.getMessage());
		}
	}

	@PutMapping
	@Operation(summary = "Update")
	@Override
	public Response<R> update(@RequestBody E e) {
		try {
			return service.update(e);
		} catch (Exception e1) {
			log.error("Error is {}, {}", e1.getMessage(), e1);
			return getErrorResponse(e1.getMessage());
		}
	}

	@GetMapping("/{id}")
	@Operation(summary = "Find by Id")
	@Override
	public Response<R> find(@PathVariable Long id) {
		try {
			return service.find(id);
		} catch (Exception e) {
			log.error("Error is {}, {}", e.getMessage(), e);
			return getErrorResponse(e.getMessage());
		}
	}

//	@DeleteMapping
//	@ApiOperation(value = "Delete")
//	@Override
//	public Response<R> delete(@RequestBody E e) {
//		try {
//			return service.delete(e);
//		} catch (Exception e1) {
//			log.error("Error is {}, {}", e1.getMessage(), e1);
//			return getErrorResponse(e1.getMessage());
//		}
//	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete by Id")
	@Override
	public Response<R> delete(@PathVariable Long id) {
		try {
			return service.delete(id);
		} catch (Exception e1) {
			log.error("Error is {}, {}", e1.getMessage(), e1);
			return getErrorResponse(e1.getMessage());
		}
	}

}
