package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.McqTestAssignment;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface McqTestAssignmentRepo extends ServiceRepository<McqTestAssignment> {

	List<McqTestAssignment> findAllByApplicationIdAndDeletedOrderByAssignedOnDesc(Long applicationId, boolean deleted);

	List<McqTestAssignment> findAllByStatusAndScheduledEndAtBetweenAndDeleted(String status, Date from, Date to, boolean deleted);

	List<McqTestAssignment> findAllByStatusAndDeadlineAtBeforeAndDeleted(String status, Date deadlineBefore, boolean deleted);

	List<McqTestAssignment> findAllByStatusAndScheduledEndAtBeforeAndDeleted(String status, Date scheduledEndBefore, boolean deleted);

	List<McqTestAssignment> findAllByStatusAndCurrentQuestionDeadlineAtBeforeAndDeleted(String status, Date deadlineBefore, boolean deleted);
}
