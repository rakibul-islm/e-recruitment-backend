package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.UserSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepo extends ServiceRepository<UserSession> {

	Optional<UserSession> findByJti(String jti);

	List<UserSession> findAllByUser_IdAndRevokedFalse(Long userId);

	List<UserSession> findAllByRevokedFalseAndDeletedFalseAndExpiresAtAfter(Date now);

	List<UserSession> findAllByUser_IdOrderByIdDesc(Long userId);

	long countByRevokedFalseAndDeletedFalseAndExpiresAtAfter(Date now);

	@Query("SELECT COUNT(DISTINCT s.user.id) FROM UserSession s WHERE s.revoked = false AND s.deleted = false AND s.expiresAt > :now")
	long countDistinctActiveUsers(@Param("now") Date now);

	@Query("SELECT s.jti FROM UserSession s WHERE s.revoked = true AND s.expiresAt > :now")
	List<String> findRevokedJtisNotExpired(@Param("now") Date now);
}
