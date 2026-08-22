package com.bd.erecruitment.audit;

import com.bd.erecruitment.repository.PermissionRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the phase-3 read API: the audit-log:read permission gate (proves the seeded
 * permission is actually enforced, not silently bypassed like an unregistered one would be), and
 * that every write path is hard-blocked (501) regardless of who's calling, since the trail must
 * stay append-only.
 * <p>
 * The two permission-check requests set {@code .servletPath(...)} explicitly: PermissionInterceptor
 * derives the resource from {@code request.getServletPath()}, which MockMvc otherwise leaves empty
 * for a root ("/") mapped DispatcherServlet — real Tomcat returns the full path here (verified
 * manually against the running app), so this makes the simulated request match reality.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditLogControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private PermissionRepo permissionRepo;

	@Test
	void auditLogReadPermission_isSeeded() {
		assertThat(permissionRepo.findByAuthority("audit-log:read")).isNotNull();
	}

	@Test
	@WithMockUser(authorities = "user:read")
	void filter_withoutAuditLogReadPermission_isForbidden() throws Exception {
		mockMvc.perform(get("/audit-log/filter").servletPath("/audit-log/filter"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(authorities = "audit-log:read")
	void filter_withAuditLogReadPermission_isOk() throws Exception {
		mockMvc.perform(get("/audit-log/filter").servletPath("/audit-log/filter"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(authorities = "audit-log:read")
	void save_isAlwaysBlocked() throws Exception {
		mockMvc.perform(post("/audit-log").contentType("application/json").content("{}"))
				.andExpect(status().isNotImplemented());
	}

	@Test
	@WithMockUser(authorities = "audit-log:read")
	void update_isAlwaysBlocked() throws Exception {
		mockMvc.perform(put("/audit-log").contentType("application/json").content("{}"))
				.andExpect(status().isNotImplemented());
	}

	@Test
	@WithMockUser(authorities = "audit-log:read")
	void hardDelete_isAlwaysBlocked() throws Exception {
		mockMvc.perform(delete("/audit-log/delete/1"))
				.andExpect(status().isNotImplemented());
	}

	@Test
	@WithMockUser(authorities = "audit-log:read")
	void softRemove_isAlwaysBlocked() throws Exception {
		mockMvc.perform(delete("/audit-log/1"))
				.andExpect(status().isNotImplemented());
	}
}
