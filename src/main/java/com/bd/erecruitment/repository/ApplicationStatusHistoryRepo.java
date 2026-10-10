package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ApplicationStatusHistoryRepo extends ServiceRepository<ApplicationStatusHistory> {

	List<ApplicationStatusHistory> findAllByApplicationIdAndDeletedOrderByChangedOnAsc(Long applicationId, boolean deleted);

	List<ApplicationStatusHistory> findAllByStatusAndDeleted(String status, boolean deleted);

	/** Distinct applications that moved to the given status after a date, optionally limited to one organization's jobs. */
	@Query("SELECT COUNT(DISTINCT h.applicationId) FROM ApplicationStatusHistory h, Application a, JobCircular j "
		+ "WHERE a.id = h.applicationId AND j.id = a.jobCircularId AND h.status = :status AND h.deleted = false AND a.deleted = false "
		+ "AND h.changedOn > :after AND (cast(:organizationId as long) is null OR j.organizationId = :organizationId)")
	long countApplicationsMovedToSince(@Param("status") String status, @Param("after") Date after, @Param("organizationId") Long organizationId);
}
