package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.CandidateProfile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateProfileRepo extends ServiceRepository<CandidateProfile> {

	Optional<CandidateProfile> findByUserIdAndDeleted(Long userId, boolean deleted);
}
