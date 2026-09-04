package com.bd.erecruitment.repository;
import com.bd.erecruitment.entity.JobCircular;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface JobCircularRepo extends ServiceRepository<JobCircular> {

	// Used by JobAlertScheduler - "recently published" is approximated as "updated since the
	// alert's last run", since JobCircular has no separate publishedOn timestamp.
	List<JobCircular> findAllByStatusAndUpdatedOnAfterAndDeleted(String status, Date after, boolean deleted);

	// Used by AnalyticsServiceImpl.
	long countByDeleted(boolean deleted);

	long countByStatusAndDeleted(String status, boolean deleted);
}
