package com.bd.erecruitment.repository;
import com.bd.erecruitment.entity.JobCircular;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface JobCircularRepo extends ServiceRepository<JobCircular> {

	@Query("select j from JobCircular j where j.status = 'PUBLISHED' and j.deleted = false " +
		"and coalesce(j.publishedOn, j.createdOn) > :after and coalesce(j.publishedOn, j.createdOn) <= :upTo")
	List<JobCircular> findPublishedBetween(@Param("after") Date after, @Param("upTo") Date upTo);

	List<JobCircular> findAllByStatusAndApplicationDeadLineBetweenAndDeleted(String status, Date from, Date to, boolean deleted);

	long countByDeleted(boolean deleted);

	long countByStatusAndDeleted(String status, boolean deleted);
}
