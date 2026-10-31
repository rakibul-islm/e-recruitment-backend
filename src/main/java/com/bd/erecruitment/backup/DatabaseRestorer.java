package com.bd.erecruitment.backup;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseRestorer {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ColumnInfo(String name, int jdbcType) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TableInfo(String name, long rows, List<ColumnInfo> columns) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Manifest(int formatVersion, String createdAt, String createdBy, String databaseProduct, List<TableInfo> tables) {}

	public record RestoreSummary(int tableCount, long rowCount) {}

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final int BATCH_SIZE = 500;
	private static final long SEQUENCE_HEADROOM = 1000;

	private final DataSource dataSource;

	public Manifest readManifest(Path file) throws IOException {
		try (ZipFile zip = new ZipFile(file.toFile())) {
			return readManifest(zip);
		}
	}

	public void validate(Path file, Set<String> excludedTables) throws IOException, SQLException {
		try (ZipFile zip = new ZipFile(file.toFile()); Connection connection = dataSource.getConnection()) {
			Manifest manifest = readManifest(zip);
			Map<String, String> dbTables = listTables(connection, excludedTables);
			checkSchema(connection, zip, manifest, dbTables);
			insertOrder(connection, dbTables.values());
		}
	}

	public RestoreSummary restore(Path file, Set<String> excludedTables) throws IOException, SQLException {
		int tableCount = 0;
		long rowCount = 0;

		try (ZipFile zip = new ZipFile(file.toFile()); Connection connection = dataSource.getConnection()) {
			Manifest manifest = readManifest(zip);
			Map<String, String> dbTables = listTables(connection, excludedTables);
			Map<String, Map<String, String>> dbColumns = checkSchema(connection, zip, manifest, dbTables);
			List<String> order = insertOrder(connection, dbTables.values());
			Map<String, TableInfo> backupTables = new HashMap<>();
			manifest.tables().forEach(t -> backupTables.put(t.name().toLowerCase(Locale.ROOT), t));
			String quote = connection.getMetaData().getIdentifierQuoteString();

			connection.setAutoCommit(false);
			try {
				for (int i = order.size() - 1; i >= 0; i--) {
					try (Statement statement = connection.createStatement()) {
						statement.executeUpdate("DELETE FROM " + quote + order.get(i) + quote);
					}
				}
				for (String table : order) {
					TableInfo info = backupTables.get(table.toLowerCase(Locale.ROOT));
					if (info == null) continue;
					rowCount += load(connection, zip, info, table, dbColumns.get(table), quote);
					tableCount++;
				}
				connection.commit();
			} catch (IOException | SQLException | RuntimeException e) {
				connection.rollback();
				throw e;
			}

			connection.setAutoCommit(true);
			resetSequences(connection, dbTables.values(), quote);
		}
		return new RestoreSummary(tableCount, rowCount);
	}

	private Manifest readManifest(ZipFile zip) throws IOException {
		ZipEntry entry = zip.getEntry(DatabaseExporter.MANIFEST_ENTRY);
		if (entry == null) throw new IllegalArgumentException("Not a valid backup: manifest.json is missing");
		Manifest manifest;
		try (InputStream in = zip.getInputStream(entry)) {
			manifest = MAPPER.readValue(in, Manifest.class);
		}
		if (manifest.tables() == null) throw new IllegalArgumentException("Not a valid backup: manifest has no tables");
		if (manifest.formatVersion() > DatabaseExporter.FORMAT_VERSION)
			throw new IllegalArgumentException("This backup was made by a newer version of the application (format " + manifest.formatVersion() + ")");
		return manifest;
	}

	private Map<String, String> listTables(Connection connection, Set<String> excludedTables) throws SQLException {
		Map<String, String> tables = new LinkedHashMap<>();
		try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
			while (rs.next()) {
				String name = rs.getString("TABLE_NAME");
				String key = name.toLowerCase(Locale.ROOT);
				if (name.startsWith("BIN$") || excludedTables.contains(key)) continue;
				tables.put(key, name);
			}
		}
		return tables;
	}

	private Map<String, Map<String, String>> checkSchema(Connection connection, ZipFile zip, Manifest manifest, Map<String, String> dbTables) throws SQLException {
		Map<String, Map<String, String>> dbColumns = new HashMap<>();
		for (TableInfo table : manifest.tables()) {
			String dbName = dbTables.get(table.name().toLowerCase(Locale.ROOT));
			if (dbName == null) throw new IllegalArgumentException("Table '" + table.name() + "' in the backup does not exist in this database");
			if (zip.getEntry(DatabaseExporter.DATA_DIR + table.name() + ".ndjson") == null)
				throw new IllegalArgumentException("Backup is incomplete: data for table '" + table.name() + "' is missing");

			Map<String, String> columns = dbColumns.computeIfAbsent(dbName, name -> columnsOf(connection, name));
			for (ColumnInfo column : table.columns()) {
				if (!columns.containsKey(column.name().toLowerCase(Locale.ROOT)))
					throw new IllegalArgumentException("Column '" + table.name() + "." + column.name() + "' in the backup does not exist in this database");
			}
		}
		return dbColumns;
	}

	private Map<String, String> columnsOf(Connection connection, String table) {
		Map<String, String> columns = new HashMap<>();
		try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), connection.getSchema(), table, "%")) {
			while (rs.next()) {
				String name = rs.getString("COLUMN_NAME");
				columns.put(name.toLowerCase(Locale.ROOT), name);
			}
		} catch (SQLException e) {
			throw new IllegalStateException("Could not read the columns of table " + table, e);
		}
		return columns;
	}

	private List<String> insertOrder(Connection connection, java.util.Collection<String> tables) throws SQLException {
		Map<String, String> byLower = new HashMap<>();
		tables.forEach(t -> byLower.put(t.toLowerCase(Locale.ROOT), t));
		Map<String, Set<String>> parentsOf = new LinkedHashMap<>();

		for (String table : tables) {
			Set<String> parents = new TreeSet<>();
			try (ResultSet rs = connection.getMetaData().getImportedKeys(connection.getCatalog(), connection.getSchema(), table)) {
				while (rs.next()) {
					String parent = byLower.get(rs.getString("PKTABLE_NAME").toLowerCase(Locale.ROOT));
					if (parent != null && !parent.equals(table)) parents.add(parent);
				}
			}
			parentsOf.put(table, parents);
		}

		List<String> ordered = new ArrayList<>();
		Set<String> placed = new HashSet<>();
		List<String> remaining = new ArrayList<>(tables);
		remaining.sort(Comparator.comparing(String::toLowerCase));
		while (!remaining.isEmpty()) {
			List<String> ready = remaining.stream().filter(t -> placed.containsAll(parentsOf.get(t))).toList();
			if (ready.isEmpty()) throw new IllegalStateException("Cannot restore: circular foreign keys between " + remaining);
			ordered.addAll(ready);
			placed.addAll(ready);
			remaining.removeAll(ready);
		}
		return ordered;
	}

	private long load(Connection connection, ZipFile zip, TableInfo info, String table, Map<String, String> dbColumns, String quote) throws IOException, SQLException {
		List<ColumnInfo> columns = info.columns();
		StringBuilder names = new StringBuilder();
		StringBuilder marks = new StringBuilder();
		for (int i = 0; i < columns.size(); i++) {
			if (i > 0) {
				names.append(", ");
				marks.append(", ");
			}
			names.append(quote).append(dbColumns.get(columns.get(i).name().toLowerCase(Locale.ROOT))).append(quote);
			marks.append('?');
		}
		String sql = "INSERT INTO " + quote + table + quote + " (" + names + ") VALUES (" + marks + ")";

		long loaded = 0;
		try (PreparedStatement statement = connection.prepareStatement(sql);
			 InputStream in = zip.getInputStream(zip.getEntry(DatabaseExporter.DATA_DIR + info.name() + ".ndjson"));
			 MappingIterator<JsonNode> rows = MAPPER.readerFor(JsonNode.class).readValues(in)) {
			while (rows.hasNext()) {
				JsonNode row = rows.next();
				for (int i = 0; i < columns.size(); i++) {
					bind(statement, i + 1, columns.get(i).jdbcType(), row.get(columns.get(i).name()));
				}
				statement.addBatch();
				loaded++;
				if (loaded % BATCH_SIZE == 0) statement.executeBatch();
			}
			statement.executeBatch();
		}
		if (loaded != info.rows())
			throw new IllegalStateException("Backup is corrupt: table '" + info.name() + "' has " + loaded + " rows but the manifest says " + info.rows());
		return loaded;
	}

	private void bind(PreparedStatement statement, int index, int type, JsonNode value) throws SQLException {
		if (value == null || value.isNull()) {
			statement.setNull(index, type);
			return;
		}
		switch (type) {
			case Types.BIT, Types.BOOLEAN -> statement.setBoolean(index, value.asBoolean());
			case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> statement.setLong(index, value.asLong());
			case Types.NUMERIC, Types.DECIMAL -> statement.setBigDecimal(index, new BigDecimal(value.asText()));
			case Types.REAL, Types.FLOAT, Types.DOUBLE -> statement.setDouble(index, value.asDouble());
			case Types.TIMESTAMP -> statement.setTimestamp(index, Timestamp.from(Instant.parse(value.asText())));
			case Types.TIMESTAMP_WITH_TIMEZONE -> statement.setObject(index, OffsetDateTime.ofInstant(Instant.parse(value.asText()), ZoneOffset.UTC));
			case Types.DATE -> statement.setDate(index, Date.valueOf(value.asText()));
			case Types.TIME -> statement.setTime(index, Time.valueOf(value.asText()));
			case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> statement.setBytes(index, Base64.getDecoder().decode(value.asText()));
			default -> statement.setString(index, value.asText());
		}
	}

	private void resetSequences(Connection connection, java.util.Collection<String> tables, String quote) throws SQLException {
		long maxId = 0;
		for (String table : tables) {
			if (!columnsOf(connection, table).containsKey("id")) continue;
			try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT MAX(" + quote + "id" + quote + ") FROM " + quote + table + quote)) {
				if (rs.next()) maxId = Math.max(maxId, rs.getLong(1));
			} catch (SQLException e) {
				try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT MAX(id) FROM " + quote + table + quote)) {
					if (rs.next()) maxId = Math.max(maxId, rs.getLong(1));
				}
			}
		}

		boolean oracle = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT).contains("oracle");
		List<String> sequences = new ArrayList<>();
		String listSql = oracle
				? "SELECT sequence_name FROM user_sequences"
				: "SELECT sequence_name FROM information_schema.sequences WHERE sequence_schema = ?";
		try (PreparedStatement statement = connection.prepareStatement(listSql)) {
			if (!oracle) statement.setString(1, connection.getSchema());
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) sequences.add(rs.getString(1));
			}
		}

		long next = maxId + SEQUENCE_HEADROOM;
		List<String> failed = new ArrayList<>();
		for (String sequence : sequences) {
			String ddl = "ALTER SEQUENCE " + quote + sequence + quote + (oracle ? " RESTART START WITH " : " RESTART WITH ") + next;
			try (Statement statement = connection.createStatement()) {
				statement.execute(ddl);
			} catch (SQLException e) {
				log.warn("[DatabaseRestorer] could not reset sequence {}: {}", sequence, e.getMessage());
				failed.add(sequence);
			}
		}
		if (!failed.isEmpty())
			throw new IllegalStateException("Data was restored, but these sequences could not be reset and must be advanced manually: " + failed);
	}
}
