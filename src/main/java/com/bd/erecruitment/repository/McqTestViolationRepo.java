package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.McqTestViolation;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface McqTestViolationRepo extends ServiceRepository<McqTestViolation> {

	long countByAssignmentIdAndDeleted(Long assignmentId, boolean deleted);

	Optional<McqTestViolation> findFirstByAssignmentIdAndDeletedOrderByIdDesc(Long assignmentId, boolean deleted);
}
