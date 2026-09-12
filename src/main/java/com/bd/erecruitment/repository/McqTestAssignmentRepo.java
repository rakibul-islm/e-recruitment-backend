package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.McqTestAssignment;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface McqTestAssignmentRepo extends ServiceRepository<McqTestAssignment> {

	List<McqTestAssignment> findAllByApplicationIdAndDeletedOrderByAssignedOnDesc(Long applicationId, boolean deleted);

	// Used by McqTestAssignmentExpirySweeper to catch abandoned/lost sessions whose client-side
	// auto-submit never fired.
	List<McqTestAssignment> findAllByStatusAndDeadlineAtBeforeAndDeleted(String status, Date deadlineBefore, boolean deleted);

	// Used by McqTestAssignmentExpirySweeper to catch scheduled exams whose "not after" start
	// window closed while the candidate never started (still ASSIGNED).
	List<McqTestAssignment> findAllByStatusAndScheduledEndAtBeforeAndDeleted(String status, Date scheduledEndBefore, boolean deleted);

	// Used by McqTestAssignmentExpirySweeper to catch a per-question timer that ran out without the
	// client-side auto-advance firing (lost connection, closed tab).
	List<McqTestAssignment> findAllByStatusAndCurrentQuestionDeadlineAtBeforeAndDeleted(String status, Date deadlineBefore, boolean deleted);
}
