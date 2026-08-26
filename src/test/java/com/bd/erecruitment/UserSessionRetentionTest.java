package com.bd.erecruitment;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.entity.UserSession;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.repository.UserSessionRepo;
import com.bd.erecruitment.retention.GenericArchiveEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// Proves GenericArchiveEngine, driven by the seeded USER_SESSION archive config (date column
// expires_at), archives then removes only sessions expired past retention - never one still valid.
@SpringBootTest
class UserSessionRetentionTest {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private UserSessionRepo userSessionRepo;

	@Autowired
	private ArchiveConfigRepo archiveConfigRepo;

	@Autowired
	private GenericArchiveEngine archiveEngine;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void archive_movesOnlyRowsPastRetentionWindow() {
		User user = saveUser();
		UserSession staleExpired = saveSession(user, daysAgo(200));
		UserSession recentlyExpired = saveSession(user, daysAgo(1));
		UserSession stillActive = saveSession(user, daysFromNow(30));

		archiveEngine.archive(configFor("USER_SESSION"));

		assertThat(userSessionRepo.findById(staleExpired.getId())).isEmpty();
		assertThat(userSessionRepo.findById(recentlyExpired.getId())).isPresent();
		assertThat(userSessionRepo.findById(stillActive.getId())).isPresent();
	}

	@Test
	void archive_copiesStaleRowIntoArchiveTableUnderItsOriginalIdBeforeDeletingIt() {
		User user = saveUser();
		UserSession staleExpired = saveSession(user, daysAgo(200));

		archiveEngine.archive(configFor("USER_SESSION"));

		String jti = jdbcTemplate.queryForObject(
				"SELECT jti FROM archive.USER_SESSION WHERE id = ?", String.class, staleExpired.getId());
		Long userId = jdbcTemplate.queryForObject(
				"SELECT user_id FROM archive.USER_SESSION WHERE id = ?", Long.class, staleExpired.getId());
		assertThat(jti).isEqualTo(staleExpired.getJti());
		assertThat(userId).isEqualTo(user.getId());
	}

	private ArchiveConfig configFor(String sourceTable) {
		return archiveConfigRepo.findBySourceTableAndDeleted(sourceTable, false)
				.orElseThrow(() -> new IllegalStateException("Missing seeded archive config for " + sourceTable));
	}

	private User saveUser() {
		User user = new User().setEmail("session-retention-" + System.nanoTime() + "@example.com").setActive(true);
		Date now = new Date();
		user.setCreatedBy("system").setCreatedOn(now).setUpdatedBy("system").setUpdatedOn(now).setDeleted(false);
		return userRepo.save(user);
	}

	private UserSession saveSession(User user, Date expiresAt) {
		UserSession session = new UserSession()
				.setUser(user)
				.setJti("jti-" + System.nanoTime())
				.setIssuedAt(daysAgo(201))
				.setExpiresAt(expiresAt)
				.setRevoked(false);
		session.setCreatedBy("system").setCreatedOn(daysAgo(201))
				.setUpdatedBy("system").setUpdatedOn(daysAgo(201))
				.setDeleted(false);
		return userSessionRepo.save(session);
	}

	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		return cal.getTime();
	}

	private Date daysFromNow(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, days);
		return cal.getTime();
	}
}
