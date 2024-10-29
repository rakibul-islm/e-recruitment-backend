package com.bd.erecruitment.controller;

import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.impl.CommonFunctionsImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequiredArgsConstructor
public class AbstractBaseController<R, E> extends CommonFunctionsImpl implements BaseController<R, E> {

	protected final BaseService<R, E> service;

	@GetMapping
	@Operation(summary = "Get All")
	@Override
	public Response<R> getAll(
			@Nullable Pageable pageable,
			@RequestParam(required = false) Boolean isPageable
	) {
		try {
			return service.getAll(pageable, isPageable);
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

	@DeleteMapping
	@Operation(summary = "Delete")
	@Override
	public Response<R> delete(@RequestBody E e) {
		try {
			return service.delete(e);
		} catch (Exception e1) {
			log.error("Error is {}, {}", e1.getMessage(), e1);
			return getErrorResponse(e1.getMessage());
		}
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove by Id")
	@Override
	public Response<R> remove(@PathVariable Long id) {
		try {
			return service.remove(id);
		} catch (Exception e1) {
			log.error("Error is {}, {}", e1.getMessage(), e1);
			return getErrorResponse(e1.getMessage());
		}
	}

}
