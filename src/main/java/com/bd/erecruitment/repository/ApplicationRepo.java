package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Application;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepo extends ServiceRepository<Application> {

	List<Application> findAllByCandidateUserIdAndDeletedOrderByAppliedOnDesc(Long candidateUserId, boolean deleted);

	Optional<Application> findByJobCircularIdAndCandidateUserIdAndDeleted(Long jobCircularId, Long candidateUserId, boolean deleted);

	// Used by AnalyticsServiceImpl for the recruitment funnel/summary.
	List<Application> findAllByDeleted(boolean deleted);

	List<Application> findAllByJobCircularIdAndDeleted(Long jobCircularId, boolean deleted);

	long countByDeleted(boolean deleted);

	long countByDeletedAndAppliedOnAfter(boolean deleted, Date after);

	@Query("SELECT a.status, COUNT(a) FROM Application a WHERE a.deleted = false GROUP BY a.status")
	List<Object[]> countGroupByStatus();
}
