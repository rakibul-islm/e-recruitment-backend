package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.CandidateCv;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateCvRepo extends ServiceRepository<CandidateCv> {

	Optional<CandidateCv> findByUserIdAndDeleted(Long userId, boolean deleted);
}
