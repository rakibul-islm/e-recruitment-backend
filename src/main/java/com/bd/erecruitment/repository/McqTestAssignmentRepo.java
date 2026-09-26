package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.McqTestAssignment;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface McqTestAssignmentRepo extends ServiceRepository<McqTestAssignment> {

	List<McqTestAssignment> findAllByApplicationIdAndDeletedOrderByAssignedOnDesc(Long applicationId, boolean deleted);

	List<McqTestAssignment> findAllByStatusAndScheduledEndAtBetweenAndDeleted(String status, Date from, Date to, boolean deleted);

	@Query("select a.id from McqTestAssignment a where a.status = :status and a.deadlineAt < :before and a.deleted = :deleted")
	List<Long> findIdsByStatusAndDeadlineAtBeforeAndDeleted(@Param("status") String status, @Param("before") Date before, @Param("deleted") boolean deleted);

	@Query("select a.id from McqTestAssignment a where a.status = :status and a.scheduledEndAt < :before and a.deleted = :deleted")
	List<Long> findIdsByStatusAndScheduledEndAtBeforeAndDeleted(@Param("status") String status, @Param("before") Date before, @Param("deleted") boolean deleted);

	@Query("select a.id from McqTestAssignment a where a.status = :status and a.currentQuestionDeadlineAt < :before and a.deleted = :deleted")
	List<Long> findIdsByStatusAndCurrentQuestionDeadlineAtBeforeAndDeleted(@Param("status") String status, @Param("before") Date before, @Param("deleted") boolean deleted);
}
