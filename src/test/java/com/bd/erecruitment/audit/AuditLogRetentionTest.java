package com.bd.erecruitment.audit;

import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves the scheduled retention purge removes only rows older than AUDIT_LOG_RETENTION_DAYS. */
@SpringBootTest
class AuditLogRetentionTest {

	@Autowired
	private AuditLogRepo auditLogRepo;

	@Autowired
	private AuditLogServiceImpl auditLogService;

	@Test
	void purgeExpiredAuditLogs_removesOnlyRowsPastRetentionWindow() {
		AuditLog stale = saveRow(daysAgo(400));
		AuditLog fresh = saveRow(daysAgo(1));

		auditLogService.purgeExpiredAuditLogs();

		assertThat(auditLogRepo.findById(stale.getId())).isEmpty();
		assertThat(auditLogRepo.findById(fresh.getId())).isPresent();
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
