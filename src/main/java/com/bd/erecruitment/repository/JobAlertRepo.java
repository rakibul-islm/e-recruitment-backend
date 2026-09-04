package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.JobAlert;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobAlertRepo extends ServiceRepository<JobAlert> {

	List<JobAlert> findAllByUserIdAndDeletedOrderByIdDesc(Long userId, boolean deleted);

	List<JobAlert> findAllByActiveAndDeleted(boolean active, boolean deleted);
}
