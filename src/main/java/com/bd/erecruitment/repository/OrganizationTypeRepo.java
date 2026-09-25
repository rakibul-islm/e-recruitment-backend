package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.OrganizationType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationTypeRepo extends ServiceRepository<OrganizationType> {

	Optional<OrganizationType> findFirstByNameIgnoreCaseAndDeleted(String name, boolean deleted);
}
