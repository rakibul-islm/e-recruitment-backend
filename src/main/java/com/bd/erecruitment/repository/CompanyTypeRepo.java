package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.CompanyType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyTypeRepo extends ServiceRepository<CompanyType> {

	Optional<CompanyType> findFirstByNameIgnoreCaseAndDeleted(String name, boolean deleted);
}
