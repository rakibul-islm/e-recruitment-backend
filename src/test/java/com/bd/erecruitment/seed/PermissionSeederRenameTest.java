package com.bd.erecruitment.seed;

import com.bd.erecruitment.entity.Permission;
import com.bd.erecruitment.repository.PermissionRepo;
import com.bd.erecruitment.seed.seeder.PermissionSeeder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermissionSeederRenameTest {

	@Test
	void existingCompanyPermissionIsRenamedInPlaceSoRolesKeepIt() {
		PermissionRepo repo = mock(PermissionRepo.class);
		Permission legacyRead = new Permission();
		legacyRead.setId(5L);
		legacyRead.setAuthority("company:read");
		legacyRead.setName("View Companies");
		when(repo.findByAuthority(anyString())).thenReturn(null);
		when(repo.findByAuthority("company:read")).thenReturn(legacyRead);

		new PermissionSeeder(repo).seed();

		ArgumentCaptor<Permission> saved = ArgumentCaptor.forClass(Permission.class);
		verify(repo, atLeastOnce()).save(saved.capture());
		List<Permission> organizationRows = saved.getAllValues().stream()
				.filter(p -> "organization:read".equals(p.getAuthority())).toList();

		assertThat(organizationRows).hasSize(1);
		assertThat(organizationRows.get(0)).isSameAs(legacyRead);
		assertThat(legacyRead.getId()).isEqualTo(5L);
		assertThat(legacyRead.getName()).isEqualTo("View Organizations");
	}
}
