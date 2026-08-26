package com.bd.erecruitment.audit;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.retention.GenericArchiveEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// Proves GenericArchiveEngine, driven by the seeded AUDIT_LOG archive config, archives then removes only rows past retention.
@SpringBootTest
class AuditLogRetentionTest {

	@Autowired
	private AuditLogRepo auditLogRepo;

	@Autowired
	private ArchiveConfigRepo archiveConfigRepo;

	@Autowired
	private GenericArchiveEngine archiveEngine;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void archive_movesOnlyRowsPastRetentionWindow() {
		AuditLog stale = saveRow(daysAgo(400));
		AuditLog fresh = saveRow(daysAgo(1));

		archiveEngine.archive(configFor("AUDIT_LOG"));

		assertThat(auditLogRepo.findById(stale.getId())).isEmpty();
		assertThat(auditLogRepo.findById(fresh.getId())).isPresent();
	}

	@Test
	void archive_copiesStaleRowIntoArchiveTableUnderItsOriginalIdBeforeDeletingIt() {
		AuditLog stale = saveRow(daysAgo(400));

		archiveEngine.archive(configFor("AUDIT_LOG"));

		String entityType = jdbcTemplate.queryForObject(
				"SELECT entity_type FROM archive.AUDIT_LOG WHERE id = ?", String.class, stale.getId());
		String createdBy = jdbcTemplate.queryForObject(
				"SELECT created_by FROM archive.AUDIT_LOG WHERE id = ?", String.class, stale.getId());
		assertThat(entityType).isEqualTo(stale.getEntityType());
		assertThat(createdBy).isEqualTo(stale.getCreatedBy());
	}

	private ArchiveConfig configFor(String sourceTable) {
		return archiveConfigRepo.findBySourceTableAndDeleted(sourceTable, false)
				.orElseThrow(() -> new IllegalStateException("Missing seeded archive config for " + sourceTable));
	}

	private AuditLog saveRow(Date createdOn) {
		AuditLog entry = new AuditLog()
				.setCategory(AuditCategory.ENTITY)
				.setAction(AuditAction.CREATE)
				.setEntityType("RetentionTest")
				.setEntityId(1L)
				.setOutcome(AuditOutcome.SUCCESS);
		entry.setCreatedBy("system").setCreatedOn(createdOn)
				.setUpdatedBy("system").setUpdatedOn(createdOn)
				.setDeleted(false);
		return auditLogRepo.save(entry);
	}

	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		return cal.getTime();
	}
}
