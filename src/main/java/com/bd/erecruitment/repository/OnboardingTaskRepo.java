package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.OnboardingTask;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface OnboardingTaskRepo extends ServiceRepository<OnboardingTask> {

	List<OnboardingTask> findAllByApplicationIdAndDeletedOrderByDueDateAsc(Long applicationId, boolean deleted);

	List<OnboardingTask> findAllByCompletedAndDueDateBetweenAndDeleted(boolean completed, Date from, Date to, boolean deleted);
}
