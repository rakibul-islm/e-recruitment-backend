package com.bd.erecruitment.backup;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@RequiredArgsConstructor
public class DatabaseExporter {

	public record ExportResult(int tableCount, long rowCount, long sizeBytes, String sha256, String databaseProduct) {}

	public static final int FORMAT_VERSION = 1;
	public static final String MANIFEST_ENTRY = "manifest.json";
	public static final String DATA_DIR = "data/";
	public static final Set<String> EXCLUDED_TABLES = Set.of("database_backup");

	private static final int FETCH_SIZE = 1000;

	private final DataSource dataSource;

	public ExportResult export(Path target, Set<String> excludedTables, String createdBy) throws IOException, SQLException {
		Path part = target.resolveSibling(target.getFileName() + ".part");
		MessageDigest digest = newDigest();
		int tableCount = 0;
		long rowCount = 0;
		String product;

		try (Connection connection = dataSource.getConnection()) {
			product = connection.getMetaData().getDatabaseProductName();
			beginSnapshot(connection);
			List<String> tables = listTables(connection, excludedTables);
			List<TableDump> dumps = new ArrayList<>();

			try (OutputStream out = new DigestOutputStream(new BufferedOutputStream(Files.newOutputStream(part)), digest);
				 ZipOutputStream zip = new ZipOutputStream(out)) {
				zip.setLevel(Deflater.BEST_SPEED);
				for (String table : tables) {
					TableDump dump = dumpTable(connection, zip, table);
					dumps.add(dump);
					rowCount += dump.rows();
					tableCount++;
				}
				writeManifest(zip, dumps, product, createdBy);
			} catch (IOException | SQLException | RuntimeException e) {
				Files.deleteIfExists(part);
				throw e;
			} finally {
				connection.rollback();
			}
		}

		Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
		return new ExportResult(tableCount, rowCount, Files.size(target), HexFormat.of().formatHex(digest.digest()), product);
	}

	private record ColumnDef(String name, int jdbcType, String typeName) {}

	private record TableDump(String name, long rows, List<ColumnDef> columns) {}

	private void beginSnapshot(Connection connection) throws SQLException {
		connection.setAutoCommit(false);
		connection.setReadOnly(true);
		try {
			connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		} catch (SQLException unsupported) {
			// Oracle has no REPEATABLE_READ; its read-only transaction already gives a consistent snapshot.
		}
	}

	private List<String> listTables(Connection connection, Set<String> excludedTables) throws SQLException {
		DatabaseMetaData metaData = connection.getMetaData();
		List<String> tables = new ArrayList<>();
		try (ResultSet rs = metaData.getTables(connection.getCatalog(), connection.getSchema(), "%", new String[]{"TABLE"})) {
			while (rs.next()) {
				String name = rs.getString("TABLE_NAME");
				if (name.startsWith("BIN$") || excludedTables.contains(name.toLowerCase(Locale.ROOT))) continue;
				tables.add(name);
			}
		}
		tables.sort(String::compareToIgnoreCase);
		return tables;
	}

	private TableDump dumpTable(Connection connection, ZipOutputStream zip, String table) throws SQLException, IOException {
		String quote = connection.getMetaData().getIdentifierQuoteString();
		long rows = 0;
		List<ColumnDef> columns = new ArrayList<>();

		zip.putNextEntry(new ZipEntry(DATA_DIR + table + ".ndjson"));
		try (Statement statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			statement.setFetchSize(FETCH_SIZE);
			try (ResultSet rs = statement.executeQuery("SELECT * FROM " + quote + table + quote)) {
				ResultSetMetaData meta = rs.getMetaData();
				for (int i = 1; i <= meta.getColumnCount(); i++) {
					columns.add(new ColumnDef(meta.getColumnName(i), meta.getColumnType(i), meta.getColumnTypeName(i)));
				}
				JsonGenerator gen = newGenerator(zip);
				while (rs.next()) {
					gen.writeStartObject();
					for (int i = 1; i <= columns.size(); i++) {
						gen.writeFieldName(columns.get(i - 1).name());
						writeValue(gen, rs, i, columns.get(i - 1).jdbcType());
					}
					gen.writeEndObject();
					rows++;
				}
				gen.flush();
			}
		}
		zip.closeEntry();
		return new TableDump(table, rows, columns);
	}

	private void writeValue(JsonGenerator gen, ResultSet rs, int i, int type) throws SQLException, IOException {
		switch (type) {
			case Types.BIT, Types.BOOLEAN -> {
				boolean value = rs.getBoolean(i);
				if (rs.wasNull()) gen.writeNull(); else gen.writeBoolean(value);
			}
			case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT -> {
				long value = rs.getLong(i);
				if (rs.wasNull()) gen.writeNull(); else gen.writeNumber(value);
			}
			case Types.NUMERIC, Types.DECIMAL -> {
				var value = rs.getBigDecimal(i);
				if (value == null) gen.writeNull(); else gen.writeNumber(value.toPlainString());
			}
			case Types.REAL, Types.FLOAT, Types.DOUBLE -> {
				double value = rs.getDouble(i);
				if (rs.wasNull()) gen.writeNull(); else gen.writeNumber(value);
			}
			case Types.TIMESTAMP -> {
				var value = rs.getTimestamp(i);
				if (value == null) gen.writeNull(); else gen.writeString(value.toInstant().toString());
			}
			case Types.TIMESTAMP_WITH_TIMEZONE -> {
				var value = rs.getObject(i, OffsetDateTime.class);
				if (value == null) gen.writeNull(); else gen.writeString(value.toInstant().toString());
			}
			case Types.DATE -> {
				var value = rs.getDate(i);
				if (value == null) gen.writeNull(); else gen.writeString(value.toString());
			}
			case Types.TIME -> {
				var value = rs.getTime(i);
				if (value == null) gen.writeNull(); else gen.writeString(value.toString());
			}
			case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> {
				byte[] value = rs.getBytes(i);
				if (value == null) gen.writeNull(); else gen.writeString(Base64.getEncoder().encodeToString(value));
			}
			default -> {
				String value = rs.getString(i);
				if (value == null) gen.writeNull(); else gen.writeString(value);
			}
		}
	}

	private void writeManifest(ZipOutputStream zip, List<TableDump> dumps, String product, String createdBy) throws IOException {
		zip.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
		JsonGenerator gen = new JsonFactory().createGenerator(zip).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET).useDefaultPrettyPrinter();
		gen.writeStartObject();
		gen.writeNumberField("formatVersion", FORMAT_VERSION);
		gen.writeStringField("createdAt", Instant.now().toString());
		gen.writeStringField("createdBy", createdBy);
		gen.writeStringField("databaseProduct", product);
		gen.writeArrayFieldStart("tables");
		for (TableDump dump : dumps) {
			gen.writeStartObject();
			gen.writeStringField("name", dump.name());
			gen.writeNumberField("rows", dump.rows());
			gen.writeArrayFieldStart("columns");
			for (ColumnDef column : dump.columns()) {
				gen.writeStartObject();
				gen.writeStringField("name", column.name());
				gen.writeNumberField("jdbcType", column.jdbcType());
				gen.writeStringField("typeName", column.typeName());
				gen.writeEndObject();
			}
			gen.writeEndArray();
			gen.writeEndObject();
		}
		gen.writeEndArray();
		gen.writeEndObject();
		gen.close();
		zip.closeEntry();
	}

	private JsonGenerator newGenerator(OutputStream out) throws IOException {
		JsonGenerator gen = new JsonFactory().createGenerator(out).disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
		gen.setRootValueSeparator(new SerializedString("\n"));
		return gen;
	}

	private MessageDigest newDigest() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
