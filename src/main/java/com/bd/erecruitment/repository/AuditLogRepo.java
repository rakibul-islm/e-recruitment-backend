package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.AuditLog;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;

@Repository
public interface AuditLogRepo extends ServiceRepository<AuditLog> {

	@Modifying
	@Transactional
	@Query("DELETE FROM AuditLog a WHERE a.createdOn < :threshold")
	int deleteByCreatedOnBefore(@Param("threshold") Date threshold);
}
