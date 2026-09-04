package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.SavedJob;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepo extends ServiceRepository<SavedJob> {

	List<SavedJob> findAllByUserIdAndDeletedOrderBySavedOnDesc(Long userId, boolean deleted);

	Optional<SavedJob> findByUserIdAndJobCircularIdAndDeleted(Long userId, Long jobCircularId, boolean deleted);
}
