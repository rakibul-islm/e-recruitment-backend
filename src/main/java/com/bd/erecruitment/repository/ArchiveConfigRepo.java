package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.ArchiveConfig;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArchiveConfigRepo extends ServiceRepository<ArchiveConfig> {

	Optional<ArchiveConfig> findBySourceTableAndDeleted(String sourceTable, boolean deleted);

	List<ArchiveConfig> findAllByEnabledTrueAndDeletedFalse();
}
