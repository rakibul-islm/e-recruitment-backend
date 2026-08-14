package com.bd.erecruitment.controller;

import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.impl.CommonFunctionsImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class AbstractBaseController<R, E> extends CommonFunctionsImpl implements BaseController<R, E>, ResponseWrapper {

	protected final BaseService<R, E> service;

	private static final Set<String> RESERVED_PARAMS = Set.of("page", "size", "sort", "isPageable");

	@GetMapping("/filter")
	@Operation(summary = "Filter")
	@Override
	public ResponseEntity<Response<R>> filter(
			@RequestParam Map<String, String> filters,
			@Nullable Pageable pageable,
			@RequestParam(required = false) Boolean isPageable
	) {
		Map<String, String> cleanFilters = new HashMap<>(filters);
		RESERVED_PARAMS.forEach(cleanFilters::remove);
		Response<R> result = service.filter(cleanFilters, pageable, isPageable);
		if (result == null) returnNotFoundException("Filter not supported");
		return respond(result);
	}

	@PostMapping
	@Operation(summary = "Save")
	@Override
	public ResponseEntity<Response<R>> save(@RequestBody E e) {
		return respond(service.save(e));
	}

	@PutMapping
	@Operation(summary = "Update")
	@Override
	public ResponseEntity<Response<R>> update(@RequestBody E e) {
		return respond(service.update(e));
	}

	@GetMapping("/{id}")
	@Operation(summary = "Find by Id")
	@Override
	public ResponseEntity<Response<R>> find(@PathVariable Long id) {
		return respond(service.find(id));
	}

	@DeleteMapping("/delete/{id}")
	@Operation(summary = "Delete permanently")
	@Override
	public ResponseEntity<Response<R>> delete(@PathVariable Long id) {
		return respond(service.delete(id));
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Remove from list")
	@Override
	public ResponseEntity<Response<R>> remove(@PathVariable Long id) {
		return respond(service.remove(id));
	}
}
