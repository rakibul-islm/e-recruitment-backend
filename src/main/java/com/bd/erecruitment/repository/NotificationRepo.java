package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepo extends ServiceRepository<Notification> {

	interface PollSummary {
		Long getUnreadCount();

		Long getLatestId();
	}

	Optional<Notification> findByIdAndRecipientUserIdAndDeleted(Long id, Long recipientUserId, boolean deleted);

	boolean existsByRecipientUserIdAndDedupeKey(Long recipientUserId, String dedupeKey);

	List<Notification> findByRecipientUserIdAndDeletedAndIdLessThanOrderByIdDesc(Long recipientUserId, boolean deleted, Long beforeId, Pageable pageable);

	List<Notification> findByRecipientUserIdAndDeletedAndReadOnIsNullAndIdLessThanOrderByIdDesc(Long recipientUserId, boolean deleted, Long beforeId, Pageable pageable);

	@Query("select count(case when n.readOn is null then 1 end) as unreadCount, max(n.id) as latestId " +
		   "from Notification n where n.recipientUserId = :userId and n.deleted = false")
	PollSummary summarize(@Param("userId") Long userId);

	@Modifying
	@Query("update Notification n set n.readOn = :now, n.updatedOn = :now, n.updatedBy = :actor " +
		   "where n.recipientUserId = :userId and n.deleted = false and n.readOn is null")
	int markAllRead(@Param("userId") Long userId, @Param("now") Date now, @Param("actor") String actor);
}
