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

@Slf4j
@Component
public class GenericArchiveEngine {

	private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
	private static final Pattern UNSAFE_CONDITION = Pattern.compile("[;]|--|/\\*");
	private static final int BATCH_SIZE = 500;
	private static final String ID_COLUMN = "id";
	private static final String DEFAULT_DATE_COLUMN = "created_on";

	private final JdbcTemplate jdbcTemplate;

	public GenericArchiveEngine(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

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

	private Object readCell(ResultSet rs, String column) throws java.sql.SQLException {
		Object value = rs.getObject(column);
		return value instanceof Clob clob ? clob.getSubString(1, (int) clob.length()) : value;
	}

	public static String requireValidIdentifier(String value, String label) {
		if (StringUtils.isBlank(value) || !IDENTIFIER.matcher(value).matches()) {
			throw new IllegalArgumentException("Invalid " + label + " in archive config: " + value);
		}
		return value;
	}

	public static String requireSafeCondition(String condition) {
		if (StringUtils.isNotBlank(condition) && UNSAFE_CONDITION.matcher(condition).find()) {
			throw new IllegalArgumentException("Where condition must not contain ';', '--', or '/*'");
		}
		return condition;
	}

	private void ensureArchiveTableExists(String archiveSchema, String archiveTable, String archiveTableRef, String sourceTable) {
		if (tableExists(archiveSchema, archiveTable)) return;
		log.info("[GenericArchiveEngine] {} does not exist yet, creating it (structure cloned from {})", archiveTableRef, sourceTable);
		jdbcTemplate.execute("CREATE TABLE " + archiveTableRef + " AS SELECT * FROM " + sourceTable + " WHERE 1 = 0");
	}

	// Uses JDBC metadata, not a failing SELECT: on Postgres one failed statement aborts the whole transaction.
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
