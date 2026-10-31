package com.bd.erecruitment.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseExporterTest {

	@TempDir
	Path tempDir;

	@Test
	void exportsEveryTableWithTypedValuesAndAVerifiableChecksum() throws Exception {
		DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:exporter_test;DB_CLOSE_DELAY=-1", "sa", "");
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE TABLE sample (id BIGINT PRIMARY KEY, name VARCHAR(50), active BOOLEAN, amount DECIMAL(10,2), created_on TIMESTAMP, payload VARBINARY(100), note CLOB)");
		jdbc.update("INSERT INTO sample VALUES (1, 'alpha', TRUE, 12.50, TIMESTAMP '2026-01-02 03:04:05', X'0102FF', 'long text')");
		jdbc.update("INSERT INTO sample VALUES (2, NULL, NULL, NULL, NULL, NULL, NULL)");
		jdbc.execute("CREATE TABLE \"Mixed Case\" (id INT PRIMARY KEY)");
		jdbc.execute("CREATE TABLE database_backup (id INT PRIMARY KEY)");

		Path target = tempDir.resolve("backup.zip");
		DatabaseExporter.ExportResult result = new DatabaseExporter(dataSource).export(target, Set.of("database_backup"), "tester");

		assertEquals(2, result.tableCount());
		assertEquals(2, result.rowCount());
		assertEquals(Files.size(target), result.sizeBytes());
		assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(target))), result.sha256());
		assertFalse(Files.exists(tempDir.resolve("backup.zip.part")));

		try (ZipFile zip = new ZipFile(target.toFile())) {
			assertNotNull(zip.getEntry(DatabaseExporter.MANIFEST_ENTRY));
			assertNotNull(zip.getEntry("data/Mixed Case.ndjson"));
			assertEquals(null, zip.getEntry("data/DATABASE_BACKUP.ndjson"));

			List<String> rows = readLines(zip, zip.getEntry("data/SAMPLE.ndjson"));
			assertEquals(2, rows.size());
			assertTrue(rows.get(0).startsWith("{\"ID\":1,\"NAME\":\"alpha\",\"ACTIVE\":true,\"AMOUNT\":12.50,\"CREATED_ON\":\"2026-01-0"));
			assertTrue(rows.get(0).endsWith("Z\",\"PAYLOAD\":\"AQL/\",\"NOTE\":\"long text\"}"));
			assertEquals("{\"ID\":2,\"NAME\":null,\"ACTIVE\":null,\"AMOUNT\":null,\"CREATED_ON\":null,\"PAYLOAD\":null,\"NOTE\":null}", rows.get(1));

			String manifest = new String(zip.getInputStream(zip.getEntry(DatabaseExporter.MANIFEST_ENTRY)).readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(manifest.contains("\"formatVersion\" : 1"));
			assertTrue(manifest.contains("\"createdBy\" : \"tester\""));
		}
	}

	private List<String> readLines(ZipFile zip, ZipEntry entry) throws Exception {
		return new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8).lines().toList();
	}
}
