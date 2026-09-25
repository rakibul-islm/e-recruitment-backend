package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Organization;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationRepo extends ServiceRepository<Organization> {

	Optional<Organization> findFirstByNameIgnoreCaseAndDeleted(String name, boolean deleted);
}
