package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.PasswordPolicy;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordPolicyRepo extends ServiceRepository<PasswordPolicy> {
	Optional<PasswordPolicy> findFirstByDeletedFalseOrderByIdAsc();
}
