package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.ArchiveConfigReqDto;
import com.bd.erecruitment.dto.res.ArchiveConfigResDTO;
import com.bd.erecruitment.dto.res.ArchivedDataResDTO;
import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.retention.GenericArchiveEngine;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ArchiveConfigServiceImpl extends AbstractBaseService<ArchiveConfig> implements BaseService<ArchiveConfigResDTO, ArchiveConfigReqDto> {

	// The config table itself is never a sensible archive source.
	private static final String EXCLUDED_TABLE = "ARCHIVE_CONFIG";

	// Catalog/system schemas an admin would never pick as an archive destination.
	private static final Set<String> SYSTEM_SCHEMAS = Set.of(
			"INFORMATION_SCHEMA", "PG_CATALOG", "PG_TOAST", "PG_TOAST_TEMP_1", "PG_TEMP_1", "SYS", "SYSTEM");

	private static final Set<Integer> DATE_SQL_TYPES = Set.of(Types.DATE, Types.TIMESTAMP, Types.TIMESTAMP_WITH_TIMEZONE, Types.TIME);

	private final ArchiveConfigRepo archiveConfigRepo;
	private final JdbcTemplate jdbcTemplate;
	private final GenericArchiveEngine archiveEngine;

	// Lazily filled, like PasswordPolicyServiceImpl's cachedPolicy - the schema doesn't change while running.
	private volatile List<String> cachedSourceTables;
	private volatile List<String> cachedArchiveSchemas;
	private final Map<String, List<String>> cachedDateColumnsByTable = new ConcurrentHashMap<>();

	ArchiveConfigServiceImpl(ArchiveConfigRepo archiveConfigRepo, JdbcTemplate jdbcTemplate, GenericArchiveEngine archiveEngine) {
		super(archiveConfigRepo);
		this.archiveConfigRepo = archiveConfigRepo;
		this.jdbcTemplate = jdbcTemplate;
		this.archiveEngine = archiveEngine;
	}

	// Read directly from the repository (no caching) - the scheduler only calls this once per run.
	public List<ArchiveConfig> findEnabled() {
		return archiveConfigRepo.findAllByEnabledTrueAndDeletedFalse();
	}

	// Lists tables in the app's default schema via JDBC metadata, so the dropdown reflects reality, not a hardcoded list.
	public List<String> listSourceTables() {
		List<String> cached = cachedSourceTables;
		if (cached != null) return cached;

		List<String> tables = jdbcTemplate.execute((ConnectionCallback<List<String>>) connection -> {
			List<String> found = new ArrayList<>();
			try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), connection.getSchema(), "%", new String[] { "TABLE" })) {
				while (rs.next()) found.add(rs.getString("TABLE_NAME"));
			}
			return found;
		});
		List<String> loaded = tables.stream()
				.filter(table -> !EXCLUDED_TABLE.equalsIgnoreCase(table))
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		cachedSourceTables = loaded;
		return loaded;
	}

	// Lists real DB schemas (minus catalog/system ones) via JDBC metadata, for the archive schema dropdown.
	public List<String> listArchiveSchemas() {
		List<String> cached = cachedArchiveSchemas;
		if (cached != null) return cached;

		List<String> schemas = jdbcTemplate.execute((ConnectionCallback<List<String>>) connection -> {
			List<String> found = new ArrayList<>();
			try (ResultSet rs = connection.getMetaData().getSchemas()) {
				while (rs.next()) found.add(rs.getString("TABLE_SCHEM"));
			}
			return found;
		});
		List<String> loaded = schemas.stream()
				.filter(schema -> !SYSTEM_SCHEMAS.contains(schema.toUpperCase()))
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
		cachedArchiveSchemas = loaded;
		return loaded;
	}

	// Lists a source table's own date/timestamp columns, for the date column dropdown - cached per
	// table for the same reason as listSourceTables(): the schema doesn't change while running.
	// Table/schema are matched case-insensitively in Java, not passed as exact-case metadata
	// patterns - H2/Oracle fold unquoted identifiers to uppercase but Postgres folds to lowercase,
	// so a literal uppercase table name would silently match nothing there (see GenericArchiveEngine#tableExists).
	public List<String> listDateColumns(String sourceTable) {
		String table = GenericArchiveEngine.requireValidIdentifier(sourceTable, "source table");
		return cachedDateColumnsByTable.computeIfAbsent(table.toUpperCase(), key -> jdbcTemplate.execute((ConnectionCallback<List<String>>) connection -> {
			String defaultSchema = connection.getSchema();
			List<String> found = new ArrayList<>();
			try (ResultSet rs = connection.getMetaData().getColumns(connection.getCatalog(), null, "%", "%")) {
				while (rs.next()) {
					if (!table.equalsIgnoreCase(rs.getString("TABLE_NAME"))) continue;
					if (defaultSchema != null && !defaultSchema.equalsIgnoreCase(rs.getString("TABLE_SCHEM"))) continue;
					if (DATE_SQL_TYPES.contains(rs.getInt("DATA_TYPE"))) found.add(rs.getString("COLUMN_NAME"));
				}
			}
			return found.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
		}));
	}

	@Transactional
	@Override
	public Response<ArchiveConfigResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Found", new ArchiveConfigResDTO(findByIdOrThrow(id, "Archive config not found")));
	}

	@Transactional
	@Override
	public Response<ArchiveConfigResDTO> save(ArchiveConfigReqDto reqDto) {
		validateForm(reqDto);
		ArchiveConfig saved = createEntity(reqDto.getBean());
		return getCreatedResponse("Saved successfully", new ArchiveConfigResDTO(saved));
	}

	@Transactional
	@Override
	public Response<ArchiveConfigResDTO> update(ArchiveConfigReqDto reqDto) {
		if (reqDto.getId() == null) returnErrorException("Id required");
		validateForm(reqDto);
		ArchiveConfig existing = findByIdOrThrow(reqDto.getId(), "Archive config not found");
		existing.setSourceTable(reqDto.getSourceTable()).setArchiveSchema(reqDto.getArchiveSchema())
				.setArchiveTable(reqDto.getArchiveTable()).setDateColumn(reqDto.getDateColumn())
				.setRetentionDays(reqDto.getRetentionDays()).setEnabled(reqDto.isEnabled())
				.setDescription(reqDto.getDescription()).setWhereCondition(reqDto.getWhereCondition());
		return getSuccessResponse("Updated successfully", new ArchiveConfigResDTO(updateEntity(existing)));
	}

	@Transactional
	@Override
	public Response<ArchiveConfigResDTO> delete(Long id) {
		deleteEntity(findByIdOrThrow(id, "Archive config not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<ArchiveConfigResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Archive config not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<ArchiveConfigResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, ArchiveConfigResDTO.class);
	}

	// Runs this config's archive job immediately, regardless of its enabled flag (that only gates the scheduler).
	@Transactional
	public Response<Integer> archiveNow(Long id) {
		ArchiveConfig config = findByIdOrThrow(id, "Archive config not found");
		int archived = archiveEngine.archive(config);
		return getSuccessResponse(archived + " row(s) archived", archived);
	}

	// Read-only browse of this config's archive table, for the "View Archived Data" page.
	public Response<ArchivedDataResDTO> findArchivedData(Long id, int page, int size) {
		ArchiveConfig config = findByIdOrThrow(id, "Archive config not found");
		int clampedSize = Math.min(Math.max(size, 1), 100);
		int clampedPage = Math.max(page, 0);
		GenericArchiveEngine.ArchivedPage result = archiveEngine.findArchivedRows(config, clampedPage, clampedSize);
		ArchivedDataResDTO dto = new ArchivedDataResDTO(result.columns(), result.rows(), result.totalElements(), clampedPage, clampedSize);
		return getSuccessResponse("Found", dto);
	}

	private void validateForm(ArchiveConfigReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getSourceTable())) returnErrorException("Source table required");
		if (StringUtils.isBlank(reqDto.getArchiveSchema())) returnErrorException("Archive schema required");
		if (StringUtils.isBlank(reqDto.getArchiveTable())) returnErrorException("Archive table required");
		if (reqDto.getRetentionDays() <= 0) returnErrorException("Retention days must be positive");
		try {
			GenericArchiveEngine.requireSafeCondition(reqDto.getWhereCondition());
		} catch (IllegalArgumentException ex) {
			returnErrorException(ex.getMessage());
		}
	}
}
