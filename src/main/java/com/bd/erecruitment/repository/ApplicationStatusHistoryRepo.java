package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface ApplicationStatusHistoryRepo extends ServiceRepository<ApplicationStatusHistory> {

	List<ApplicationStatusHistory> findAllByApplicationIdAndDeletedOrderByChangedOnAsc(Long applicationId, boolean deleted);

	List<ApplicationStatusHistory> findAllByStatusAndDeleted(String status, boolean deleted);

	long countByStatusAndDeletedAndChangedOnAfter(String status, boolean deleted, Date after);

	/** One row per status-change: [changedOn, appliedOn]. Avoids loading entities or a lookup per application. */
	@Query("SELECT h.changedOn, a.appliedOn FROM ApplicationStatusHistory h, Application a "
		+ "WHERE a.id = h.applicationId AND h.status = :status AND h.deleted = false AND a.deleted = false "
		+ "AND h.changedOn IS NOT NULL AND a.appliedOn IS NOT NULL")
	List<Object[]> findChangedOnAndAppliedOnByStatus(String status);
}
