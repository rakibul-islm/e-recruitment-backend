package com.bd.erecruitment;

import com.bd.erecruitment.service.impl.ArchiveConfigServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Proves listSourceTables() reads real tables from the schema rather than a hardcoded list.
@SpringBootTest
class ArchiveConfigSourceTablesTest {

	@Autowired
	private ArchiveConfigServiceImpl archiveConfigService;

	@Test
	void listSourceTables_returnsRealTablesButExcludesTheConfigTableItself() {
		List<String> tables = archiveConfigService.listSourceTables();

		assertThat(tables).contains("AUDIT_LOG", "EXCEPTION_LOG", "USER_SESSION");
		assertThat(tables).doesNotContain("ARCHIVE_CONFIG");
	}
}
