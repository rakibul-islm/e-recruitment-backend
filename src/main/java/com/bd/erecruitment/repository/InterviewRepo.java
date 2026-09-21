package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Interview;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface InterviewRepo extends ServiceRepository<Interview> {

	List<Interview> findAllByApplicationIdAndDeletedOrderByScheduledAtAsc(Long applicationId, boolean deleted);

	@Query("SELECT DISTINCT i FROM Interview i LEFT JOIN FETCH i.interviewerUserIds " +
		   "WHERE i.status = :status AND i.scheduledAt BETWEEN :from AND :to AND i.deleted = false")
	List<Interview> findWithInterviewersByStatusAndScheduledAtBetween(@Param("status") String status, @Param("from") Date from, @Param("to") Date to);
}
