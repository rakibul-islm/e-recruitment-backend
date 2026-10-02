package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.UserSession;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepo extends ServiceRepository<UserSession> {

	Optional<UserSession> findByJti(String jti);

	List<UserSession> findAllByUser_IdAndRevokedFalse(Long userId);

	List<UserSession> findAllByRevokedFalseAndDeletedFalseAndExpiresAtAfter(Date now);

	List<UserSession> findAllByUser_IdOrderByIdDesc(Long userId);

	@Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.user.id IN :userIds AND s.revoked = false AND s.deleted = false AND s.expiresAt > :now ORDER BY s.issuedAt DESC")
	List<UserSession> findActiveByUserIds(@Param("userIds") Collection<Long> userIds, @Param("now") Date now);

	@Query("SELECT s FROM UserSession s JOIN FETCH s.user WHERE s.revoked = false AND s.deleted = false AND s.expiresAt > :now ORDER BY s.issuedAt DESC")
	List<UserSession> findAllActive(@Param("now") Date now);

	long countByRevokedFalseAndDeletedFalseAndExpiresAtAfter(Date now);

	@Query("SELECT s.jti FROM UserSession s WHERE s.revoked = true AND s.expiresAt > :now")
	List<String> findRevokedJtisNotExpired(@Param("now") Date now);
}
