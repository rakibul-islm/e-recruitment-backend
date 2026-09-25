package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.ApplicationStatusHistory;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationStatusHistoryRepo extends ServiceRepository<ApplicationStatusHistory> {

	List<ApplicationStatusHistory> findAllByApplicationIdAndDeletedOrderByChangedOnAsc(Long applicationId, boolean deleted);

	List<ApplicationStatusHistory> findAllByStatusAndDeleted(String status, boolean deleted);
}
