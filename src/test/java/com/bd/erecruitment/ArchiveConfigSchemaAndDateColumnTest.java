package com.bd.erecruitment;

import com.bd.erecruitment.service.impl.ArchiveConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Proves the archive-schema and date-column dropdown sources read real DB metadata, not a hardcoded list.
@SpringBootTest
class ArchiveConfigSchemaAndDateColumnTest {

	@Autowired
	private ArchiveConfigServiceImpl archiveConfigService;

	@Test
	void listArchiveSchemas_includesTheProvisionedArchiveSchemaButExcludesSystemSchemas() {
		List<String> schemas = archiveConfigService.listArchiveSchemas();

		assertThat(schemas).anyMatch(s -> s.equalsIgnoreCase("archive"));
		assertThat(schemas).noneMatch(s -> s.equalsIgnoreCase("information_schema"));
	}

	@Test
	void listDateColumns_returnsOnlyDateOrTimestampColumnsOfTheGivenTable() {
		List<String> columns = archiveConfigService.listDateColumns("AUDIT_LOG");

		assertThat(columns).anyMatch(c -> c.equalsIgnoreCase("created_on"));
		assertThat(columns).noneMatch(c -> c.equalsIgnoreCase("entity_type"));
	}
}
