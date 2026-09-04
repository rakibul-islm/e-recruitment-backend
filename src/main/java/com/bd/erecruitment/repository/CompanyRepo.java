package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Company;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepo extends ServiceRepository<Company> {

	Optional<Company> findFirstByNameIgnoreCaseAndDeleted(String name, boolean deleted);
}
