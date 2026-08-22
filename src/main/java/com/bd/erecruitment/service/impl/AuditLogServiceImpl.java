package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.AuditLogReqDto;
import com.bd.erecruitment.dto.res.AuditLogResDTO;
import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Read-only, append-only: save/update/delete/remove are all blocked at the controller (501) so
 * the audit trail can't be tampered with through the API. The only thing that ever removes rows
 * is the scheduled retention purge below, which operates on a raw age threshold, not per-row.
 */
@Slf4j
@Service
public class AuditLogServiceImpl extends AbstractBaseService<AuditLog> implements BaseService<AuditLogResDTO, AuditLogReqDto> {

	private static final String RETENTION_CONFIG_KEY = "AUDIT_LOG_RETENTION_DAYS";
	private static final int DEFAULT_RETENTION_DAYS = 365;

	private final AuditLogRepo auditLogRepo;
	private final SystemConfigServiceImpl systemConfigService;

	AuditLogServiceImpl(AuditLogRepo auditLogRepo, SystemConfigServiceImpl systemConfigService) {
		super(auditLogRepo);
		this.auditLogRepo = auditLogRepo;
		this.systemConfigService = systemConfigService;
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

	@Scheduled(cron = "0 0 3 * * *")
	public void purgeExpiredAuditLogs() {
		Date threshold = thresholdDate();
		int deleted = auditLogRepo.deleteByCreatedOnBefore(threshold);
		if (deleted > 0) log.info("[AuditLogServiceImpl] purged {} audit log row(s) older than {}", deleted, threshold);
	}

	private Date thresholdDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -retentionDays());
		return cal.getTime();
	}

	private int retentionDays() {
		SystemConfig config = systemConfigService.findCachedByKey(RETENTION_CONFIG_KEY);
		return config == null ? DEFAULT_RETENTION_DAYS : NumberUtils.toInt(config.getConfigValue(), DEFAULT_RETENTION_DAYS);
	}
}
