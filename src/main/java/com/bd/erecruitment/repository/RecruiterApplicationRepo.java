package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.RecruiterApplication;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecruiterApplicationRepo extends ServiceRepository<RecruiterApplication> {

	Optional<RecruiterApplication> findFirstByEmailAndStatusAndDeleted(String email, String status, boolean deleted);
}
