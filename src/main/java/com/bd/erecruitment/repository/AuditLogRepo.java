package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.AuditLog;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepo extends ServiceRepository<AuditLog> {
}
