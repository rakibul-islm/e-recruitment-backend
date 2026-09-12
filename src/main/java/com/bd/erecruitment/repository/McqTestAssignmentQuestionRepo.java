package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.McqTestAssignmentQuestion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McqTestAssignmentQuestionRepo extends ServiceRepository<McqTestAssignmentQuestion> {

	List<McqTestAssignmentQuestion> findAllByAssignmentIdAndDeletedOrderByDisplayOrderAsc(Long assignmentId, boolean deleted);

	// Scopes an answer write to a specific assignment so a candidate can't post an answer against
	// a question row belonging to someone else's attempt.
	Optional<McqTestAssignmentQuestion> findByIdAndAssignmentIdAndDeleted(Long id, Long assignmentId, boolean deleted);
}
