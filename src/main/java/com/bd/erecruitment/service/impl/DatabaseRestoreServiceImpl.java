package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.audit.AuditAction;
import com.bd.erecruitment.audit.AuditLogWriter;
import com.bd.erecruitment.backup.DatabaseExporter;
import com.bd.erecruitment.backup.DatabaseRestorer;
import com.bd.erecruitment.backup.RestoreState;
import com.bd.erecruitment.dto.res.RestoreStatusResDTO;
import com.bd.erecruitment.entity.DatabaseBackup;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.security.PermissionInterceptor;
import com.bd.erecruitment.security.UserDetailsCache;
import com.bd.erecruitment.util.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executor;

@Slf4j
@Service
public class DatabaseRestoreServiceImpl extends CommonFunctionsImpl {

	private static final String CONFIRMATION_WORD = "RESTORE";
	private static final String ENTITY_TYPE = "DatabaseBackup";

	private final DatabaseBackupServiceImpl backupService;
	private final DatabaseRestorer restorer;
	private final RestoreState restoreState;
	private final AuditLogWriter auditLogWriter;
	private final SystemConfigServiceImpl systemConfigService;
	private final UserDetailsCache userDetailsCache;
	private final PermissionInterceptor permissionInterceptor;
	private final Executor backupExecutor;

	public DatabaseRestoreServiceImpl(DatabaseBackupServiceImpl backupService,
									  DatabaseRestorer restorer,
									  RestoreState restoreState,
									  AuditLogWriter auditLogWriter,
									  SystemConfigServiceImpl systemConfigService,
									  UserDetailsCache userDetailsCache,
									  PermissionInterceptor permissionInterceptor,
									  @Qualifier("backupExecutor") Executor backupExecutor) {
		this.backupService = backupService;
		this.restorer = restorer;
		this.restoreState = restoreState;
		this.auditLogWriter = auditLogWriter;
		this.systemConfigService = systemConfigService;
		this.userDetailsCache = userDetailsCache;
		this.permissionInterceptor = permissionInterceptor;
		this.backupExecutor = backupExecutor;
	}

	public Response<RestoreStatusResDTO> status() {
		return getSuccessResponse("Found", new RestoreStatusResDTO(restoreState.get()));
	}

	public Response<RestoreStatusResDTO> start(Long id, String confirmation) {
		if (!CONFIRMATION_WORD.equals(confirmation)) returnErrorException("Type " + CONFIRMATION_WORD + " to confirm the restore");

		DatabaseBackupServiceImpl.BackupFile file = backupService.locateForRestore(id);
		MyUserDetail me = getLoggedInUserDetails();
		String actor = me == null ? "system" : me.getUsername();
		if (!restoreState.tryBegin(file.fileName(), actor)) returnErrorException("A restore is already running");

		auditLogWriter.logActivity(AuditAction.RESTORE, ENTITY_TYPE, id, "started from " + file.fileName());
		try {
			backupExecutor.execute(() -> run(id, actor));
		} catch (TaskRejectedException e) {
			restoreState.fail("Another backup operation is busy; try again shortly");
			returnErrorException("Another backup operation is busy; try again shortly");
		}
		return getSuccessResponse("Restore started", new RestoreStatusResDTO(restoreState.get()));
	}

	private void run(Long id, String actor) {
		String fileName = restoreState.get().fileName();
		try {
			DatabaseBackupServiceImpl.BackupFile file = verify(id);
			restoreState.markRunning();
			DatabaseBackup safety = backupService.snapshotNow(actor, "pre-restore");
			DatabaseRestorer.RestoreSummary summary = restorer.restore(file.path(), DatabaseExporter.EXCLUDED_TABLES);
			clearCaches();
			String message = "Restored " + summary.tableCount() + " tables and " + summary.rowCount() + " rows. Your previous data was saved as " + safety.getFileName() + ".";
			restoreState.complete(message);
			auditLogWriter.logEntitySync(AuditAction.RESTORE, ENTITY_TYPE, id, AuditOutcome.SUCCESS, "restored from " + fileName + " by " + actor);
		} catch (Exception e) {
			log.error("[DatabaseRestore] restore from {} failed: {}", fileName, e.getMessage(), e);
			clearCaches();
			restoreState.fail(e.getMessage());
			auditLogWriter.logEntitySync(AuditAction.RESTORE, ENTITY_TYPE, id, AuditOutcome.FAILURE, "restore from " + fileName + " failed: " + e.getMessage());
		}
	}

	private DatabaseBackupServiceImpl.BackupFile verify(Long id) {
		DatabaseBackupServiceImpl.BackupFile file = backupService.prepareRestore(id);
		try {
			restorer.validate(file.path(), DatabaseExporter.EXCLUDED_TABLES);
		} catch (Exception e) {
			throw new IllegalStateException("This backup cannot be restored: " + e.getMessage(), e);
		}
		return file;
	}

	private void clearCaches() {
		userDetailsCache.clear();
		permissionInterceptor.invalidateCache();
		systemConfigService.clearCache();
	}
}
