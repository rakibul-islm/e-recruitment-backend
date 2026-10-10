package com.bd.erecruitment.repository;
import com.bd.erecruitment.entity.JobCircular;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	/** Rows of {id, jobTitle} for non-deleted jobs, for batch-filling list responses. */
	@Query("select j.id, j.jobTitle from JobCircular j where j.deleted = false and j.id in :ids")
	List<Object[]> findTitlesByIds(@Param("ids") java.util.Collection<Long> ids);

	@Query("select count(j) from JobCircular j where j.deleted = false "
		+ "and (cast(:status as string) is null or j.status = :status) "
		+ "and (cast(:organizationId as long) is null or j.organizationId = :organizationId)")
	long countScoped(@Param("status") String status, @Param("organizationId") Long organizationId);

	@Query(value = "select j.id, j.jobTitle, j.status from JobCircular j where j.deleted = false "
		+ "and (cast(:organizationId as long) is null or j.organizationId = :organizationId) order by j.id desc",
		countQuery = "select count(j) from JobCircular j where j.deleted = false "
		+ "and (cast(:organizationId as long) is null or j.organizationId = :organizationId)")
	Page<Object[]> findJobRows(@Param("organizationId") Long organizationId, Pageable pageable);
}
