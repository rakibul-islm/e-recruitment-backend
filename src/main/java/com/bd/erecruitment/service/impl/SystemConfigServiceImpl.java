package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.SystemConfigReqDto;
import com.bd.erecruitment.dto.res.SystemConfigResDTO;
import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.repository.SystemConfigRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SystemConfigServiceImpl extends AbstractBaseService<SystemConfig> implements BaseService<SystemConfigResDTO, SystemConfigReqDto> {

	private final SystemConfigRepo systemConfigRepo;

	// Lazily filled, kept in sync on every write below; read by hot paths like ExceptionLogWriter.
	private final Map<String, SystemConfig> configCache = new ConcurrentHashMap<>();

	SystemConfigServiceImpl(SystemConfigRepo systemConfigRepo) {
		super(systemConfigRepo);
		this.systemConfigRepo = systemConfigRepo;
	}

	public SystemConfig findCachedByKey(String configKey) {
		return configCache.computeIfAbsent(configKey, key -> systemConfigRepo.findByConfigKeyAndDeleted(key, false).orElse(null));
	}

	@Transactional
	@Override
	public Response<SystemConfigResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Found", new SystemConfigResDTO(findByIdOrThrow(id, "Config not found")));
	}

	@Transactional
	@Override
	public Response<SystemConfigResDTO> save(SystemConfigReqDto reqDto) {
		validateForm(reqDto);
		SystemConfig saved = createEntity(reqDto.getBean());
		configCache.put(saved.getConfigKey(), saved);
		return getCreatedResponse("Saved successfully", new SystemConfigResDTO(saved));
	}

	@Transactional
	@Override
	public Response<SystemConfigResDTO> update(SystemConfigReqDto reqDto) {
		if (reqDto.getId() == null) returnErrorException("Id required");
		validateForm(reqDto);
		SystemConfig existing = findByIdOrThrow(reqDto.getId(), "Config not found");
		String previousKey = existing.getConfigKey();
		existing.setConfigKey(reqDto.getConfigKey()).setConfigValue(reqDto.getConfigValue())
			.setDescription(reqDto.getDescription()).setExpectedValues(reqDto.getExpectedValues());
		SystemConfig updated = updateEntity(existing);
		if (!updated.getConfigKey().equals(previousKey)) configCache.remove(previousKey);
		configCache.put(updated.getConfigKey(), updated);
		return getSuccessResponse("Updated successfully", new SystemConfigResDTO(updated));
	}

	@Transactional
	@Override
	public Response<SystemConfigResDTO> delete(Long id) {
		SystemConfig existing = findByIdOrThrow(id, "Config not found");
		deleteEntity(existing);
		configCache.remove(existing.getConfigKey());
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<SystemConfigResDTO> remove(Long id) {
		SystemConfig existing = findByIdOrThrow(id, "Config not found");
		removeEntity(existing);
		configCache.remove(existing.getConfigKey());
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<SystemConfigResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, SystemConfigResDTO.class);
	}

	private void validateForm(SystemConfigReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getConfigKey())) returnErrorException("Config key required");
		if (StringUtils.isBlank(reqDto.getConfigValue())) returnErrorException("Config value required");
	}
}
