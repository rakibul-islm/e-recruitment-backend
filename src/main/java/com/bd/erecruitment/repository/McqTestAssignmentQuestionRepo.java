package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.McqTestAssignmentQuestion;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface McqTestAssignmentQuestionRepo extends ServiceRepository<McqTestAssignmentQuestion> {

	List<McqTestAssignmentQuestion> findAllByAssignmentIdAndDeletedOrderByDisplayOrderAsc(Long assignmentId, boolean deleted);

	Optional<McqTestAssignmentQuestion> findByIdAndAssignmentIdAndDeleted(Long id, Long assignmentId, boolean deleted);
}
