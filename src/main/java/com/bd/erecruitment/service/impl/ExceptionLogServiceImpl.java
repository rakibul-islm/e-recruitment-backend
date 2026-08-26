package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.ExceptionLogReqDto;
import com.bd.erecruitment.dto.res.ExceptionLogResDTO;
import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

// Retention archiving lives in ArchiveScheduler/GenericArchiveEngine, not this class.
@Service
public class ExceptionLogServiceImpl extends AbstractBaseService<ExceptionLog> implements BaseService<ExceptionLogResDTO, ExceptionLogReqDto> {

	ExceptionLogServiceImpl(ExceptionLogRepo exceptionLogRepo) {
		super(exceptionLogRepo);
	}

	@Transactional
	@Override
	public Response<ExceptionLogResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Found", new ExceptionLogResDTO(findByIdOrThrow(id, "Exception log not found")));
	}

	@Override
	public Response<ExceptionLogResDTO> save(ExceptionLogReqDto reqDto) { return null; }

	@Override
	public Response<ExceptionLogResDTO> update(ExceptionLogReqDto reqDto) { return null; }

	@Transactional
	@Override
	public Response<ExceptionLogResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "Exception log not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<ExceptionLogResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Exception log not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<ExceptionLogResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, ExceptionLogResDTO.class);
	}
}
