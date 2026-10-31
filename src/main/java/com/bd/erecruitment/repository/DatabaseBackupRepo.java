package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.DatabaseBackup;
import com.bd.erecruitment.enums.BackupStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseBackupRepo extends ServiceRepository<DatabaseBackup> {

	List<DatabaseBackup> findAllByOrderByIdDesc();

	boolean existsByStatus(BackupStatus status);

	List<DatabaseBackup> findAllByStatus(BackupStatus status);

	List<DatabaseBackup> findAllByDriveStatus(String driveStatus);

	Optional<DatabaseBackup> findFirstByFileNameStartingWithOrderByIdDesc(String prefix);

	List<DatabaseBackup> findAllByFileNameStartingWithAndStatusOrderByIdDesc(String prefix, BackupStatus status);
}
