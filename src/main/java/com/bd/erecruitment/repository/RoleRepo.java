package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepo extends ServiceRepository<Role> {
	Role findByCode(String code);
	List<Role> findByDeletedFalse();

	@Override
	@EntityGraph(attributePaths = { "permissions" })
	Optional<Role> findByIdAndDeleted(Long id, boolean deleted);
}
