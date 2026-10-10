package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Application;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepo extends ServiceRepository<Application> {

	List<Application> findAllByCandidateUserIdAndDeletedOrderByAppliedOnDesc(Long candidateUserId, boolean deleted);

	Optional<Application> findByJobCircularIdAndCandidateUserIdAndDeleted(Long jobCircularId, Long candidateUserId, boolean deleted);

	List<Application> findAllByDeleted(boolean deleted);

	List<Application> findAllByJobCircularIdAndDeleted(Long jobCircularId, boolean deleted);

	@Query("SELECT a.jobCircularId, COUNT(a) FROM Application a WHERE a.jobCircularId IN :jobCircularIds AND a.deleted = false GROUP BY a.jobCircularId")
	List<Object[]> countGroupByJobCircularIdIn(List<Long> jobCircularIds);

	String SCOPED_FROM = "from Application a join JobCircular j on j.id = a.jobCircularId "
		+ "join User u on u.id = a.candidateUserId "
		+ "where a.deleted = false "
		+ "and (cast(:organizationId as long) is null or j.organizationId = :organizationId) ";

	String DETAIL_FROM = SCOPED_FROM
		+ "and (cast(:appliedAfter as timestamp) is null or a.appliedOn >= :appliedAfter) ";

	@Query(value = "select a.id, j.id, j.jobTitle, u.fullName, u.email, a.status, a.appliedOn " + DETAIL_FROM
		+ "order by a.appliedOn desc, a.id desc", countQuery = "select count(a) " + DETAIL_FROM)
	Page<Object[]> findDetailRows(@Param("appliedAfter") Date appliedAfter,
		@Param("organizationId") Long organizationId, Pageable pageable);

	@Query("select count(a) " + SCOPED_FROM + "and a.appliedOn >= :since")
	long countAppliedSince(@Param("since") Date since, @Param("organizationId") Long organizationId);

	@Query("select a.status, count(a) " + SCOPED_FROM + "and (cast(:jobCircularId as long) is null or j.id = :jobCircularId) group by a.status")
	List<Object[]> countGroupByStatus(@Param("jobCircularId") Long jobCircularId, @Param("organizationId") Long organizationId);

	@Query("select a.statusUpdatedOn, a.appliedOn " + SCOPED_FROM
		+ "and a.status = 'HIRED' and a.statusUpdatedOn is not null and a.appliedOn is not null")
	List<Object[]> findHiredDates(@Param("organizationId") Long organizationId);

	@Query("select a.status, count(a) from Application a where a.candidateUserId = :userId and a.deleted = false group by a.status")
	List<Object[]> countGroupByStatusForCandidate(@Param("userId") Long userId);

	@Query("select a.id, j.jobTitle, a.status, a.appliedOn from Application a left join JobCircular j on j.id = a.jobCircularId "
		+ "where a.candidateUserId = :userId and a.deleted = false order by a.appliedOn desc, a.id desc")
	List<Object[]> findRecentForCandidate(@Param("userId") Long userId, Pageable pageable);
}
