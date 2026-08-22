package com.bd.erecruitment.audit;

import com.bd.erecruitment.dto.req.SystemConfigReqDto;
import com.bd.erecruitment.dto.res.SystemConfigResDTO;
import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.service.impl.SystemConfigServiceImpl;
import com.bd.erecruitment.util.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the AbstractBaseService -> AuditLogWriter hook wired in phase 1: entity-CRUD audit
 * rows deferred to after the enclosing transaction commits, and hard-delete's synchronous
 * same-transaction write.
 */
@SpringBootTest
class AuditLogWriterIntegrationTest {

	private static final String ACTOR_EMAIL = "audit-test@example.com";

	@Autowired
	private SystemConfigServiceImpl systemConfigService;

	@Autowired
	private AuditLogRepo auditLogRepo;

	@BeforeEach
	void authenticate() {
		User user = User.builder().email(ACTOR_EMAIL).active(true).expiryDate(farFutureDate()).build();
		MyUserDetail principal = new MyUserDetail(user);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	@AfterEach
	void clearAuthentication() {
		SecurityContextHolder.clearContext();
	}

	@Test
	@Transactional
	void createEntity_writesAuditRowOnlyAfterCommit() throws InterruptedException {
		Long savedId = saveTestConfig("AUDIT_TEST_CREATE_");

		assertThat(findAuditRows(savedId, AuditAction.CREATE)).isEmpty();

		TestTransaction.flagForCommit();
		TestTransaction.end();

		AuditLog row = awaitAuditRow(savedId, AuditAction.CREATE);
		assertThat(row.getCategory()).isEqualTo(AuditCategory.ENTITY);
		assertThat(row.getEntityType()).isEqualTo("SystemConfig");
		assertThat(row.getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
		assertThat(row.getCreatedBy()).isEqualTo(ACTOR_EMAIL);
	}

	@Test
	@Transactional
	void rolledBackTransaction_neverWritesAuditRow() throws InterruptedException {
		Long savedId = saveTestConfig("AUDIT_TEST_ROLLBACK_");

		TestTransaction.flagForRollback();
		TestTransaction.end();

		Thread.sleep(300);
		assertThat(findAuditRows(savedId, AuditAction.CREATE)).isEmpty();
	}

	@Test
	@Transactional
	void hardDelete_writesAuditRowSynchronouslyInSameTransaction() {
		Long savedId = saveTestConfig("AUDIT_TEST_HARD_DELETE_");

		systemConfigService.delete(savedId);

		assertThat(findAuditRows(savedId, AuditAction.HARD_DELETE)).hasSize(1);
	}

	private Long saveTestConfig(String keyPrefix) {
		SystemConfigReqDto dto = new SystemConfigReqDto();
		dto.setConfigKey(keyPrefix + System.nanoTime());
		dto.setConfigValue("Y");
		Response<SystemConfigResDTO> response = systemConfigService.save(dto);
		return response.getObj().getId();
	}

	private List<AuditLog> findAuditRows(Long entityId, String action) {
		return auditLogRepo.findAll().stream()
				.filter(a -> entityId.equals(a.getEntityId()) && action.equals(a.getAction()))
				.toList();
	}

	private AuditLog awaitAuditRow(Long entityId, String action) throws InterruptedException {
		for (int i = 0; i < 30; i++) {
			List<AuditLog> matches = findAuditRows(entityId, action);
			if (!matches.isEmpty()) return matches.get(0);
			Thread.sleep(100);
		}
		throw new AssertionError("No audit row found for entityId=" + entityId + " action=" + action + " within timeout");
	}

	private Date farFutureDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 10);
		return cal.getTime();
	}
}
