package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepo extends ServiceRepository<User> {
	User findByEmail(String email);
	User findByGoogleId(String googleId);
	User findByActivationToken(String activationToken);

	@Query("SELECT u.id FROM User u WHERE u.deleted = false AND u.active = true")
	List<Long> findAllActiveIds();

	@Query("SELECT DISTINCT u.id FROM User u JOIN u.roles r " +
		   "WHERE u.deleted = false AND u.active = true AND r.id = :roleId AND r.deleted = false")
	List<Long> findActiveIdsByRoleId(@Param("roleId") Long roleId);

	@Query("SELECT u FROM User u WHERE u.deleted = false AND u.active = true AND " +
		   "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
	List<User> searchActiveByKeyword(@Param("keyword") String keyword, Pageable pageable);

	/** Rows of {userId, roleId, roleName, roleCode, roleDescription} for the given users, for batch-filling list responses. */
	@Query("SELECT u.id, r.id, r.name, r.code, r.description FROM User u JOIN u.roles r WHERE u.id IN :userIds")
	List<Object[]> findRoleSummariesByUserIds(@Param("userIds") Collection<Long> userIds);

	/** Rows of {id, fullName, email} for non-deleted users, for batch-filling list responses. */
	@Query("SELECT u.id, u.fullName, u.email FROM User u WHERE u.deleted = false AND u.id IN :ids")
	List<Object[]> findNameSummariesByIds(@Param("ids") Collection<Long> ids);

	@Override
	@EntityGraph(attributePaths = { "roles" })
	Optional<User> findByIdAndDeleted(Long id, boolean deleted);

	@Query("SELECT DISTINCT u.id FROM User u JOIN u.roles r JOIN r.permissions p " +
		   "WHERE u.deleted = false AND u.active = true AND r.deleted = false AND p.authority IN :authorities")
	List<Long> findActiveIdsByAnyAuthority(@Param("authorities") Collection<String> authorities);

	@Query("SELECT DISTINCT u FROM User u " +
		   "LEFT JOIN FETCH u.roles ur LEFT JOIN FETCH ur.permissions " +
		   "WHERE u.email = :login AND u.deleted = false")
	Optional<User> findByLoginWithPermissions(@Param("login") String login);
}
