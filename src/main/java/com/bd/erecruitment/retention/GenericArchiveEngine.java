package com.bd.erecruitment.retention;

import com.bd.erecruitment.entity.ArchiveConfig;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Copies rows past retention into their archive table (name-matched columns, no JPA entity involved), then deletes them.
@Slf4j
@Component
public class GenericArchiveEngine {

	private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
	// Blocks statement-stacking/comment bypass in the where-condition text - not a full SQL parser, just a floor.
	private static final Pattern UNSAFE_CONDITION = Pattern.compile("[;]|--|/\\*");
	private static final int BATCH_SIZE = 500;
	private static final String ID_COLUMN = "id";
	private static final String DEFAULT_DATE_COLUMN = "created_on";

	private final JdbcTemplate jdbcTemplate;

	public GenericArchiveEngine(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	// One transaction per config, so a failure can't leave a batch copied-but-not-deleted or vice versa.
	@Transactional
	public int archive(ArchiveConfig config) {
		String sourceTable = requireValidIdentifier(config.getSourceTable(), "source table");
		String archiveSchema = requireValidIdentifier(config.getArchiveSchema(), "archive schema");
		String archiveTable = requireValidIdentifier(config.getArchiveTable(), "archive table");
		String archiveTableRef = archiveSchema + "." + archiveTable;
		String dateColumn = requireValidIdentifier(
				StringUtils.isBlank(config.getDateColumn()) ? DEFAULT_DATE_COLUMN : config.getDateColumn(), "date column");

		String whereCondition = requireSafeCondition(config.getWhereCondition());

		ensureArchiveTableExists(archiveSchema, archiveTable, archiveTableRef, sourceTable);
		List<String> columns = sharedColumns(sourceTable, archiveTableRef);
		if (columns.isEmpty()) {
			log.error("[GenericArchiveEngine] {}: no columns shared with archive table {}, skipping", sourceTable, archiveTableRef);
			return 0;
		}
		String columnList = String.join(", ", columns);
		Date threshold = Date.from(Instant.now().minus(config.getRetentionDays(), ChronoUnit.DAYS));

		// Only narrows this SELECT - INSERT/DELETE afterward operate on the exact id list it produced.
		String extraCondition = StringUtils.isBlank(whereCondition) ? "" : " AND (" + whereCondition + ")";
		String selectIdsSql = "SELECT " + ID_COLUMN + " FROM " + sourceTable + " WHERE " + dateColumn
				+ " < ?" + extraCondition + " ORDER BY " + ID_COLUMN + " FETCH FIRST " + BATCH_SIZE + " ROWS ONLY";

		int total = 0;
		List<Long> ids;
		while (!(ids = jdbcTemplate.queryForList(selectIdsSql, Long.class, threshold)).isEmpty()) {
			String placeholders = ids.stream().map(id -> "?").collect(Collectors.joining(","));
			Object[] idArgs = ids.toArray();
			jdbcTemplate.update("INSERT INTO " + archiveTableRef + " (" + columnList + ") SELECT " + columnList
					+ " FROM " + sourceTable + " WHERE " + ID_COLUMN + " IN (" + placeholders + ")", idArgs);
			jdbcTemplate.update("DELETE FROM " + sourceTable + " WHERE " + ID_COLUMN + " IN (" + placeholders + ")", idArgs);
			total += ids.size();
		}
		return total;
	}

	public record ArchivedPage(List<String> columns, List<Map<String, Object>> rows, long totalElements) {}

	// Read-only browse of one config's archive table, for the "View Archived Data" page - no
	// entity involved, same as everything else here. Columns come back even for an empty table
	// (unlike deriving them from the first row) so the UI can still render a header.
	public ArchivedPage findArchivedRows(ArchiveConfig config, int page, int size) {
		String archiveSchema = requireValidIdentifier(config.getArchiveSchema(), "archive schema");
		String archiveTable = requireValidIdentifier(config.getArchiveTable(), "archive table");
		String archiveTableRef = archiveSchema + "." + archiveTable;

		if (!tableExists(archiveSchema, archiveTable)) {
			return new ArchivedPage(List.of(), List.of(), 0);
		}

		List<String> columns = columnsOf(archiveTableRef);
		long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + archiveTableRef, Long.class);

		String columnList = String.join(", ", columns);
		String sql = "SELECT " + columnList + " FROM " + archiveTableRef + " ORDER BY " + ID_COLUMN + " DESC OFFSET ? ROWS FETCH FIRST ? ROWS ONLY";
		List<Map<String, Object>> rows = jdbcTemplate.query(sql, (rs, rowNum) -> {
			Map<String, Object> row = new LinkedHashMap<>();
			for (String column : columns) row.put(column, readCell(rs, column));
			return row;
		}, Math.max(page, 0) * size, size);

		return new ArchivedPage(columns, rows, total);
	}

	// Oracle CLOB columns come back as a Clob handle rather than a String from getObject().
	private Object readCell(ResultSet rs, String column) throws java.sql.SQLException {
		Object value = rs.getObject(column);
		return value instanceof Clob clob ? clob.getSubString(1, (int) clob.length()) : value;
	}

	// Public so ArchiveConfigServiceImpl can validate a source table name before an admin metadata lookup too.
	public static String requireValidIdentifier(String value, String label) {
		if (StringUtils.isBlank(value) || !IDENTIFIER.matcher(value).matches()) {
			throw new IllegalArgumentException("Invalid " + label + " in archive config: " + value);
		}
		return value;
	}

	// Public so ArchiveConfigServiceImpl can reject an unsafe condition at save/update time too.
	public static String requireSafeCondition(String condition) {
		if (StringUtils.isNotBlank(condition) && UNSAFE_CONDITION.matcher(condition).find()) {
			throw new IllegalArgumentException("Where condition must not contain ';', '--', or '/*'");
		}
		return condition;
	}

	// Clones the source table's columns (no rows) via CTAS, portable across H2/Postgres/Oracle. The archive schema itself must already exist.
	// Existence is checked via JDBC metadata, never by running a SELECT expected to fail: on
	// Postgres, one failed statement aborts the whole transaction, so a later CREATE TABLE in the
	// same transaction would itself fail with "current transaction is aborted" - H2/Oracle don't
	// have that restriction, which is why this only ever surfaced against Postgres.
	private void ensureArchiveTableExists(String archiveSchema, String archiveTable, String archiveTableRef, String sourceTable) {
		if (tableExists(archiveSchema, archiveTable)) return;
		log.info("[GenericArchiveEngine] {} does not exist yet, creating it (structure cloned from {})", archiveTableRef, sourceTable);
		jdbcTemplate.execute("CREATE TABLE " + archiveTableRef + " AS SELECT * FROM " + sourceTable + " WHERE 1 = 0");
	}

	// Schema/table names are matched case-insensitively in Java rather than passed as JDBC metadata
	// patterns, since dialects fold unquoted identifiers to different cases (Postgres: lowercase,
	// H2/Oracle: uppercase) and the metadata pattern arguments aren't auto-folded like SQL identifiers are.
	private boolean tableExists(String schema, String table) {
		return jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
			try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), null, "%", new String[] { "TABLE" })) {
				while (rs.next()) {
					if (table.equalsIgnoreCase(rs.getString("TABLE_NAME")) && schema.equalsIgnoreCase(rs.getString("TABLE_SCHEM"))) {
						return true;
					}
				}
				return false;
			}
		});
	}

	private List<String> sharedColumns(String sourceTable, String archiveTableRef) {
		List<String> sourceColumns = columnsOf(sourceTable);
		Set<String> archiveColumns = columnsOf(archiveTableRef).stream().map(String::toUpperCase).collect(Collectors.toSet());
		return sourceColumns.stream().filter(c -> archiveColumns.contains(c.toUpperCase())).toList();
	}

	private List<String> columnsOf(String tableRef) {
		return jdbcTemplate.query("SELECT * FROM " + tableRef + " WHERE 1 = 0", (ResultSet rs) -> {
			ResultSetMetaData meta = rs.getMetaData();
			List<String> columns = new ArrayList<>();
			for (int i = 1; i <= meta.getColumnCount(); i++) columns.add(meta.getColumnName(i));
			return columns;
		});
	}
}
