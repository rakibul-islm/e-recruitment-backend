package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Interview;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewRepo extends ServiceRepository<Interview> {

	List<Interview> findAllByApplicationIdAndDeletedOrderByScheduledAtAsc(Long applicationId, boolean deleted);
}
