package com.bd.erecruitment.notification;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.retention.GenericArchiveEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class NotificationRetentionTest {

	@Autowired
	private NotificationRepo notificationRepo;

	@Autowired
	private ArchiveConfigRepo archiveConfigRepo;

	@Autowired
	private GenericArchiveEngine archiveEngine;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		notificationRepo.deleteAll();
	}

	@Test
	void archiveConfigForNotificationsIsSeeded() {
		ArchiveConfig config = archiveConfigRepo.findBySourceTableAndDeleted("NOTIFICATION", false).orElseThrow();

		assertThat(config.getRetentionDays()).isEqualTo(90);
		assertThat(config.getDateColumn()).isEqualTo("created_on");
		assertThat(config.isEnabled()).isTrue();
	}

	@Test
	void archive_movesOnlyNotificationsPastRetentionAndKeepsTheirContent() {
		Notification stale = saveRow(daysAgo(120), "/old");
		Notification fresh = saveRow(daysAgo(1), "/new");

		archiveEngine.archive(archiveConfigRepo.findBySourceTableAndDeleted("NOTIFICATION", false).orElseThrow());

		assertThat(notificationRepo.findById(stale.getId())).isEmpty();
		assertThat(notificationRepo.findById(fresh.getId())).isPresent();
		String archivedRoute = jdbcTemplate.queryForObject(
				"SELECT action_route FROM archive.NOTIFICATION WHERE id = ?", String.class, stale.getId());
		assertThat(archivedRoute).isEqualTo("/old");
	}

	private Notification saveRow(Date createdOn, String route) {
		Notification notification = new Notification()
				.setRecipientUserId(1L)
				.setType(NotificationType.APPLICATION_RECEIVED)
				.setActionRoute(route)
				.setParamsJson("{}");
		notification.setCreatedBy("system").setCreatedOn(createdOn).setUpdatedBy("system").setUpdatedOn(createdOn).setDeleted(false);
		return notificationRepo.save(notification);
	}

	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -days);
		return cal.getTime();
	}
}
