package com.bd.erecruitment.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseRestorerTest {

	@TempDir
	Path tempDir;

	private JdbcTemplate seededDatabase(String name) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE TABLE parent (id BIGINT PRIMARY KEY, name VARCHAR(50))");
		jdbc.execute("CREATE TABLE child (id BIGINT PRIMARY KEY, parent_id BIGINT NOT NULL, payload VARBINARY(20), seen_on TIMESTAMP, price DECIMAL(10,2), active BOOLEAN, CONSTRAINT fk_child_parent FOREIGN KEY (parent_id) REFERENCES parent(id))");
		jdbc.execute("CREATE TABLE database_backup (id INT PRIMARY KEY)");
		jdbc.execute("CREATE SEQUENCE parent_seq START WITH 1");
		jdbc.update("INSERT INTO parent VALUES (1, 'one'), (2, 'two')");
		jdbc.update("INSERT INTO child VALUES (10, 1, X'0AFF', TIMESTAMP '2026-03-04 05:06:07', 9.99, TRUE), (11, 2, NULL, NULL, NULL, NULL)");
		jdbc.update("INSERT INTO database_backup VALUES (1)");
		return jdbc;
	}

	@Test
	void restoreReplacesCurrentDataWithTheBackupAndKeepsExcludedTables() throws Exception {
		JdbcTemplate jdbc = seededDatabase("restore_roundtrip");
		DriverManagerDataSource dataSource = (DriverManagerDataSource) jdbc.getDataSource();
		Path zip = tempDir.resolve("backup.zip");
		new DatabaseExporter(dataSource).export(zip, DatabaseExporter.EXCLUDED_TABLES, "tester");

		String childQuery = "SELECT id, parent_id, RAWTOHEX(payload) AS payload, seen_on, price, active FROM child ORDER BY id";
		List<Map<String, Object>> childBefore = jdbc.queryForList(childQuery);

		jdbc.update("DELETE FROM child");
		jdbc.update("DELETE FROM parent");
		jdbc.update("INSERT INTO parent VALUES (500, 'stray')");
		jdbc.update("INSERT INTO database_backup VALUES (2)");

		DatabaseRestorer.RestoreSummary summary = new DatabaseRestorer(dataSource).restore(zip, DatabaseExporter.EXCLUDED_TABLES);

		assertEquals(2, summary.tableCount());
		assertEquals(4, summary.rowCount());
		assertEquals(List.of("one", "two"), jdbc.queryForList("SELECT name FROM parent ORDER BY id", String.class));
		assertEquals(childBefore, jdbc.queryForList(childQuery));
		assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM database_backup", Integer.class));
		assertTrue(jdbc.queryForObject("SELECT NEXT VALUE FOR parent_seq", Long.class) > 11);
	}

	@Test
	void failedRestoreRollsBackAndLeavesCurrentDataUntouched() throws Exception {
		JdbcTemplate jdbc = seededDatabase("restore_rollback");
		DriverManagerDataSource dataSource = (DriverManagerDataSource) jdbc.getDataSource();
		Path zip = tempDir.resolve("backup.zip");
		new DatabaseExporter(dataSource).export(zip, DatabaseExporter.EXCLUDED_TABLES, "tester");

		jdbc.update("INSERT INTO parent VALUES (3, 'three')");
		jdbc.update("DELETE FROM child");
		jdbc.execute("ALTER TABLE child ADD CONSTRAINT chk_price CHECK (price IS NULL OR price < 5)");

		assertThrows(Exception.class, () -> new DatabaseRestorer(dataSource).restore(zip, DatabaseExporter.EXCLUDED_TABLES));

		assertEquals(List.of("one", "two", "three"), jdbc.queryForList("SELECT name FROM parent ORDER BY id", String.class));
	}

	@Test
	void rejectsABackupWhoseSchemaDoesNotMatch() throws Exception {
		JdbcTemplate jdbc = seededDatabase("restore_schema");
		DriverManagerDataSource dataSource = (DriverManagerDataSource) jdbc.getDataSource();
		Path zip = tempDir.resolve("backup.zip");
		new DatabaseExporter(dataSource).export(zip, DatabaseExporter.EXCLUDED_TABLES, "tester");

		jdbc.execute("ALTER TABLE parent DROP COLUMN name");

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new DatabaseRestorer(dataSource).validate(zip, Set.of("database_backup")));
		assertTrue(e.getMessage().contains("PARENT.NAME"));
	}
}
