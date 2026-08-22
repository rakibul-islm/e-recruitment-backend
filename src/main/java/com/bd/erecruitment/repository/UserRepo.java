package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends ServiceRepository<User> {
	User findByEmail(String email);
	User findByGoogleId(String googleId);
	User findByActivationToken(String activationToken);

	// Fetches roles eagerly to avoid a separate lazy-load query from audit diffing or response mapping.
	@Override
	@EntityGraph(attributePaths = { "roles" })
	Optional<User> findByIdAndDeleted(Long id, boolean deleted);

	// Loads user with all permission data for Spring Security authority building
	@Query("SELECT DISTINCT u FROM User u " +
		   "LEFT JOIN FETCH u.roles ur LEFT JOIN FETCH ur.permissions " +
		   "WHERE u.email = :login AND u.deleted = false")
	Optional<User> findByLoginWithPermissions(@Param("login") String login);
}
