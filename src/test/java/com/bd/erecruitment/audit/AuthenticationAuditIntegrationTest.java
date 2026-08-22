package com.bd.erecruitment.audit;

import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the phase-2 security-event audit path end to end through the real filter chain:
 * CorrelationIdFilter assigns a correlation id, AuthenticationServiceImpl.generateToken audits
 * the failed login with the attempted (never-authenticated) email as actor, and the two are
 * tied together via correlation_id.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationAuditIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuditLogRepo auditLogRepo;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void badCredentialsLogin_writesLoginFailureAuditRowTaggedWithResponseCorrelationId() throws Exception {
		String email = "nonexistent-" + System.nanoTime() + "@example.com";
		String body = objectMapper.writeValueAsString(Map.of("email", email, "password", "wrong-password"));

		String correlationId = mockMvc.perform(post("/authenticate/token")
						.contentType("application/json")
						.content(body))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getHeader("X-Correlation-Id");

		assertThat(correlationId).isNotBlank();

		AuditLog row = awaitAuditRow(email, AuditAction.LOGIN_FAILURE);
		assertThat(row.getCategory()).isEqualTo(AuditCategory.SECURITY);
		assertThat(row.getOutcome()).isEqualTo(AuditOutcome.FAILURE);
		assertThat(row.getCorrelationId()).isEqualTo(correlationId);
	}

	private AuditLog awaitAuditRow(String actorEmail, String action) throws InterruptedException {
		for (int i = 0; i < 30; i++) {
			List<AuditLog> matches = auditLogRepo.findAll().stream()
					.filter(a -> actorEmail.equals(a.getCreatedBy()) && action.equals(a.getAction()))
					.toList();
			if (!matches.isEmpty()) return matches.get(0);
			Thread.sleep(100);
		}
		throw new AssertionError("No audit row found for actor=" + actorEmail + " action=" + action + " within timeout");
	}
}
