package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.UserGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserGroupRepo extends ServiceRepository<UserGroup> {

	Optional<UserGroup> findByNameAndDeletedFalse(String name);

	// Fetches roles eagerly to avoid a separate lazy-load query from audit diffing or response mapping.
	@Override
	@EntityGraph(attributePaths = { "roles" })
	Optional<UserGroup> findByIdAndDeleted(Long id, boolean deleted);

	@Query("SELECT DISTINCT g FROM UserGroup g " +
		   "LEFT JOIN FETCH g.roles r " +
		   "LEFT JOIN FETCH r.permissions " +
		   "WHERE g.id = :id AND g.deleted = false")
	Optional<UserGroup> findByIdWithDetails(@Param("id") Long id);
}
