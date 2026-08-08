package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.SystemConfig;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemConfigRepo extends ServiceRepository<SystemConfig> {
	Optional<SystemConfig> findByConfigKeyAndDeleted(String configKey, boolean deleted);
}
