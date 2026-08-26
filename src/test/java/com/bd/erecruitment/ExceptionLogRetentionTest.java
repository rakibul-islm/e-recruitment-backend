package com.bd.erecruitment;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.retention.GenericArchiveEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// Proves GenericArchiveEngine, driven by the seeded EXCEPTION_LOG archive config, archives then removes only rows past retention.
@SpringBootTest
class ExceptionLogRetentionTest {

	@Autowired
	private ExceptionLogRepo exceptionLogRepo;

	@Autowired
	private ArchiveConfigRepo archiveConfigRepo;

	@Autowired
	private GenericArchiveEngine archiveEngine;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void archive_movesOnlyRowsPastRetentionWindow() {
		ExceptionLog stale = saveRow(daysAgo(200));
		ExceptionLog fresh = saveRow(daysAgo(1));

		archiveEngine.archive(configFor("EXCEPTION_LOG"));

		assertThat(exceptionLogRepo.findById(stale.getId())).isEmpty();
		assertThat(exceptionLogRepo.findById(fresh.getId())).isPresent();
	}

	@Test
	void archive_copiesStaleRowIntoArchiveTableUnderItsOriginalIdBeforeDeletingIt() {
		ExceptionLog stale = saveRow(daysAgo(200));

		archiveEngine.archive(configFor("EXCEPTION_LOG"));

		String exceptionClass = jdbcTemplate.queryForObject(
				"SELECT exception_class FROM archive.EXCEPTION_LOG WHERE id = ?", String.class, stale.getId());
		assertThat(exceptionClass).isEqualTo(stale.getExceptionClass());
	}

	private ArchiveConfig configFor(String sourceTable) {
		return archiveConfigRepo.findBySourceTableAndDeleted(sourceTable, false)
				.orElseThrow(() -> new IllegalStateException("Missing seeded archive config for " + sourceTable));
	}

	private ExceptionLog saveRow(Date createdOn) {
		ExceptionLog entry = new ExceptionLog()
				.setTraceId("trace-1")
				.setExceptionClass("java.lang.RuntimeException")
				.setStatusCode(500)
				.setRequestUri("/api/test")
				.setMessage("boom");
		entry.setCreatedBy("system").setCreatedOn(createdOn)
				.setUpdatedBy("system").setUpdatedOn(createdOn)
				.setDeleted(false);
		return exceptionLogRepo.save(entry);
	}

	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		return cal.getTime();
	}
}
