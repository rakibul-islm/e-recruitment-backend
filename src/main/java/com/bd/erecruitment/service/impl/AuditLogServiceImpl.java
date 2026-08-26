package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.AuditLogReqDto;
import com.bd.erecruitment.dto.res.AuditLogResDTO;
import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

// Read-only, append-only: save/update/delete/remove are blocked at the controller (501). Retention archiving lives in ArchiveScheduler/GenericArchiveEngine.
@Service
public class AuditLogServiceImpl extends AbstractBaseService<AuditLog> implements BaseService<AuditLogResDTO, AuditLogReqDto> {

	AuditLogServiceImpl(AuditLogRepo auditLogRepo) {
		super(auditLogRepo);
	}

	@Transactional
	@Override
	public Response<AuditLogResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Found", new AuditLogResDTO(findByIdOrThrow(id, "Audit log not found")));
	}

	@Override
	public Response<AuditLogResDTO> save(AuditLogReqDto reqDto) { return null; }

	@Override
	public Response<AuditLogResDTO> update(AuditLogReqDto reqDto) { return null; }

	@Override
	public Response<AuditLogResDTO> delete(Long id) { return null; }

	@Override
	public Response<AuditLogResDTO> remove(Long id) { return null; }

	@Override
	public Response<AuditLogResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, AuditLogResDTO.class);
	}
}
