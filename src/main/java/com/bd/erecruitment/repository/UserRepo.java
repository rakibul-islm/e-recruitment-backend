package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends ServiceRepository<User> {
	User findByUsername(String username);
	User findByEmail(String email);
	User findByGoogleId(String googleId);

	// Loads user with all permission data for Spring Security authority building
	@Query("SELECT DISTINCT u FROM User u " +
		   "LEFT JOIN FETCH u.roles ur LEFT JOIN FETCH ur.permissions " +
		   "LEFT JOIN FETCH u.userGroups g LEFT JOIN FETCH g.roles gr LEFT JOIN FETCH gr.permissions " +
		   "WHERE (u.username = :login OR u.email = :login) AND u.deleted = false")
	Optional<User> findByLoginWithPermissions(@Param("login") String login);
}
