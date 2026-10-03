package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.DeviceToken;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepo extends ServiceRepository<DeviceToken> {

	Optional<DeviceToken> findByPushToken(String pushToken);

	List<DeviceToken> findByUserId(Long userId);

	@Modifying
	@Query("delete from DeviceToken t where t.pushToken = :pushToken")
	int deleteByPushToken(@Param("pushToken") String pushToken);

	@Modifying
	@Query("delete from DeviceToken t where t.pushToken = :pushToken and t.userId = :userId")
	int deleteByPushTokenAndUserId(@Param("pushToken") String pushToken, @Param("userId") Long userId);
}
