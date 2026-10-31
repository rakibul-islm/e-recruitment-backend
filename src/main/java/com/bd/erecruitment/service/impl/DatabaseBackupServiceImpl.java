package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.audit.AuditAction;
import com.bd.erecruitment.audit.AuditLogWriter;
import com.bd.erecruitment.backup.DatabaseExporter;
import com.bd.erecruitment.backup.DatabaseRestorer;
import com.bd.erecruitment.backup.GoogleDriveClient;
import com.bd.erecruitment.backup.RestoreState;
import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.dto.res.DatabaseBackupResDTO;
import com.bd.erecruitment.entity.DatabaseBackup;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.enums.BackupStatus;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.notification.NotificationPublisher;
import com.bd.erecruitment.repository.DatabaseBackupRepo;
import com.bd.erecruitment.util.RequestUtils;
import com.bd.erecruitment.util.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class DatabaseBackupServiceImpl extends CommonFunctionsImpl {

	public record BackupFile(String fileName, Path path) {}

	private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);
	private static final String ENTITY_TYPE = "DatabaseBackup";
	private static final String SCHEDULED_PREFIX = "scheduled";
	private static final String ACTION_ROUTE = "/backups";
	private static final String DRIVE_UPLOADING = "UPLOADING";
	private static final String DRIVE_UPLOADED = "UPLOADED";
	private static final String DRIVE_FAILED = "FAILED";

	private final DatabaseBackupRepo backupRepo;
	private final DatabaseExporter exporter;
	private final DatabaseRestorer restorer;
	private final RestoreState restoreState;
	private final NotificationPublisher notificationPublisher;
	private final AuditLogWriter auditLogWriter;
	private final GoogleDriveClient driveClient;
	private final SystemConfigServiceImpl systemConfigService;
	private final Executor backupExecutor;
	private final Path backupDir;

	public DatabaseBackupServiceImpl(DatabaseBackupRepo backupRepo,
									 DatabaseExporter exporter,
									 DatabaseRestorer restorer,
									 RestoreState restoreState,
									 NotificationPublisher notificationPublisher,
									 AuditLogWriter auditLogWriter,
									 GoogleDriveClient driveClient,
									 SystemConfigServiceImpl systemConfigService,
									 @Qualifier("backupExecutor") Executor backupExecutor,
									 @Value("${app.backup.dir:backups}") String backupDir) {
		this.backupRepo = backupRepo;
		this.exporter = exporter;
		this.restorer = restorer;
		this.restoreState = restoreState;
		this.driveClient = driveClient;
		this.systemConfigService = systemConfigService;
		this.notificationPublisher = notificationPublisher;
		this.auditLogWriter = auditLogWriter;
		this.backupExecutor = backupExecutor;
		this.backupDir = Path.of(backupDir).toAbsolutePath().normalize();
	}

	public Response<DatabaseBackupResDTO> list() {
		return getSuccessResponse("Found", backupRepo.findAllByOrderByIdDesc().stream().map(DatabaseBackupResDTO::new).toList());
	}

	public Response<DatabaseBackupResDTO> create() {
		if (backupRepo.existsByStatus(BackupStatus.RUNNING) || restoreState.isBusy()) returnErrorException("A backup or restore is already running");

		MyUserDetail me = getLoggedInUserDetails();
		String actor = me == null ? "system" : me.getUsername();
		Long userId = me == null ? null : me.getId();

		DatabaseBackup saved = newRecord(actor, "db-backup", BackupStatus.RUNNING);
		auditLogWriter.logActivity(AuditAction.BACKUP, ENTITY_TYPE, saved.getId(), saved.getFileName());
		backupExecutor.execute(() -> runBackup(saved.getId(), actor, userId));
		return getCreatedResponse("Backup started", new DatabaseBackupResDTO(saved));
	}

	public Optional<Date> lastScheduledAt() {
		return backupRepo.findFirstByFileNameStartingWithOrderByIdDesc(SCHEDULED_PREFIX + "-").map(DatabaseBackup::getCreatedOn);
	}

	public void startScheduled(int keepCount) {
		if (backupRepo.existsByStatus(BackupStatus.RUNNING) || restoreState.isBusy()) {
			log.warn("[DatabaseBackup] scheduled backup skipped: another backup or restore is running");
			return;
		}
		DatabaseBackup saved = newRecord("system", SCHEDULED_PREFIX, BackupStatus.RUNNING);
		try {
			backupExecutor.execute(() -> runScheduled(saved.getId(), keepCount));
		} catch (TaskRejectedException e) {
			finish(saved, BackupStatus.FAILED, "Another backup operation was busy");
		}
	}

	public DatabaseBackup snapshotNow(String actor, String prefix) {
		DatabaseBackup saved = newRecord(actor, prefix, BackupStatus.RUNNING);
		runBackup(saved.getId(), actor, null);
		DatabaseBackup done = backupRepo.findById(saved.getId()).orElseThrow();
		if (done.getStatus() != BackupStatus.COMPLETED) throw new IllegalStateException("Safety backup failed: " + done.getErrorMessage());
		return done;
	}

	public Response<DatabaseBackupResDTO> upload(MultipartFile upload) {
		if (upload == null || upload.isEmpty() || !StringUtils.endsWithIgnoreCase(upload.getOriginalFilename(), ".zip")) returnErrorException("Choose a backup .zip file");

		MyUserDetail me = getLoggedInUserDetails();
		String actor = me == null ? "system" : me.getUsername();
		DatabaseBackup record = newRecord(actor, "uploaded", BackupStatus.RUNNING);
		Path target = resolveFile(record);
		try {
			Files.createDirectories(backupDir);
			upload.transferTo(target);
			DatabaseRestorer.Manifest manifest = restorer.readManifest(target);
			record.setSizeBytes(Files.size(target))
				.setSha256(sha256Of(target))
				.setTableCount(manifest.tables().size())
				.setRowCount(manifest.tables().stream().mapToLong(DatabaseRestorer.TableInfo::rows).sum())
				.setDatabaseProduct(manifest.databaseProduct());
			finish(record, BackupStatus.COMPLETED, null);
		} catch (Exception e) {
			try {
				Files.deleteIfExists(target);
			} catch (IOException ignored) {
				log.warn("[DatabaseBackup] could not remove rejected upload {}", target);
			}
			backupRepo.delete(record);
			returnErrorException("Not a valid backup file: " + e.getMessage());
		}
		auditLogWriter.logActivity(AuditAction.BACKUP, ENTITY_TYPE, record.getId(), "uploaded " + upload.getOriginalFilename());
		return getCreatedResponse("Backup uploaded", new DatabaseBackupResDTO(record));
	}

	public BackupFile locateForRestore(Long id) {
		DatabaseBackup backup = findBackup(id);
		if (backup.getStatus() != BackupStatus.COMPLETED) returnErrorException("Only a completed backup can be restored");
		Path file = resolveFile(backup);
		if (!Files.isRegularFile(file)) returnNotFoundException("Backup file is missing on the server");
		return new BackupFile(backup.getFileName(), file);
	}

	public BackupFile prepareRestore(Long id) {
		DatabaseBackup backup = findBackup(id);
		if (backup.getStatus() != BackupStatus.COMPLETED) returnErrorException("Only a completed backup can be restored");
		if (backupRepo.existsByStatus(BackupStatus.RUNNING)) returnErrorException("A backup is running; wait for it to finish");
		Path file = resolveFile(backup);
		if (!Files.isRegularFile(file)) returnNotFoundException("Backup file is missing on the server");
		try {
			if (backup.getSha256() != null && !backup.getSha256().equals(sha256Of(file))) returnErrorException("Backup file failed its integrity check (checksum mismatch)");
		} catch (IOException e) {
			returnErrorException("Could not read the backup file");
		}
		return new BackupFile(backup.getFileName(), file);
	}

	private DatabaseBackup newRecord(String actor, String prefix, BackupStatus status) {
		Date now = new Date();
		DatabaseBackup backup = new DatabaseBackup().setFileName(prefix + "-" + FILE_STAMP.format(now.toInstant()) + ".zip").setStatus(status);
		String terminal = RequestUtils.getClientTerminal();
		backup.setDeleted(false);
		backup.setCreatedBy(actor);
		backup.setCreatedOn(now);
		backup.setCreatedTerminal(terminal);
		backup.setUpdatedBy(actor);
		backup.setUpdatedOn(now);
		backup.setUpdatedTerminal(terminal);
		return backupRepo.save(backup);
	}

	private String sha256Of(Path file) throws IOException {
		try (InputStream in = Files.newInputStream(file)) {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] buffer = new byte[65536];
			int read;
			while ((read = in.read(buffer)) > 0) digest.update(buffer, 0, read);
			return HexFormat.of().formatHex(digest.digest());
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}

	public BackupFile download(Long id) {
		DatabaseBackup backup = findBackup(id);
		if (backup.getStatus() != BackupStatus.COMPLETED) returnErrorException("Backup is not available for download");
		Path file = resolveFile(backup);
		if (!Files.isRegularFile(file)) returnNotFoundException("Backup file is missing on the server");
		auditLogWriter.logActivity(AuditAction.DOWNLOAD, ENTITY_TYPE, id, backup.getFileName());
		return new BackupFile(backup.getFileName(), file);
	}

	public Response<DatabaseBackupResDTO> delete(Long id) {
		DatabaseBackup backup = findBackup(id);
		if (backup.getStatus() == BackupStatus.RUNNING) returnErrorException("A running backup cannot be deleted");
		try {
			Files.deleteIfExists(resolveFile(backup));
		} catch (IOException e) {
			log.error("[DatabaseBackup] could not delete {}: {}", backup.getFileName(), e.getMessage(), e);
			returnErrorException("Could not delete the backup file");
		}
		deleteDriveCopy(backup);
		backupRepo.delete(backup);
		auditLogWriter.logEntity(AuditAction.HARD_DELETE, ENTITY_TYPE, id, AuditOutcome.SUCCESS, backup.getFileName());
		return getSuccessResponse("Backup deleted");
	}

	public Response<DatabaseBackupResDTO> uploadToDriveNow(Long id) {
		DatabaseBackup backup = findBackup(id);
		if (backup.getStatus() != BackupStatus.COMPLETED) returnErrorException("Only a completed backup can be uploaded");
		if (!driveClient.isConfigured()) returnErrorException("Google Drive is not configured: set the DRIVE_REFRESH_TOKEN environment variable");
		if (DRIVE_UPLOADING.equals(backup.getDriveStatus())) returnErrorException("This backup is already being uploaded");
		if (!Files.isRegularFile(resolveFile(backup))) returnNotFoundException("Backup file is missing on the server");

		backup.setDriveStatus(DRIVE_UPLOADING).setDriveError(null);
		backup.setUpdatedOn(new Date());
		backupRepo.save(backup);
		auditLogWriter.logActivity(AuditAction.BACKUP, ENTITY_TYPE, id, "uploading " + backup.getFileName() + " to Google Drive");
		try {
			backupExecutor.execute(() -> uploadToDrive(findBackup(id)));
		} catch (TaskRejectedException e) {
			backup.setDriveStatus(DRIVE_FAILED).setDriveError("Another backup operation was busy; try again shortly");
			backupRepo.save(backup);
			returnErrorException("Another backup operation was busy; try again shortly");
		}
		return getSuccessResponse("Upload to Google Drive started", new DatabaseBackupResDTO(backup));
	}

	@EventListener(ApplicationReadyEvent.class)
	public void recoverInterruptedBackups() {
		List<DatabaseBackup> interrupted = backupRepo.findAllByStatus(BackupStatus.RUNNING);
		interrupted.forEach(b -> finish(b, BackupStatus.FAILED, "Interrupted by an application restart"));
		if (!interrupted.isEmpty()) log.warn("[DatabaseBackup] marked {} interrupted backup(s) as failed", interrupted.size());
		backupRepo.findAllByDriveStatus(DRIVE_UPLOADING).forEach(b -> {
			b.setDriveStatus(DRIVE_FAILED).setDriveError("Interrupted by an application restart");
			backupRepo.save(b);
		});
		deleteLeftoverPartFiles();
	}

	private void runBackup(Long id, String actor, Long userId) {
		DatabaseBackup backup = backupRepo.findById(id).orElse(null);
		if (backup == null) return;
		try {
			Files.createDirectories(backupDir);
			DatabaseExporter.ExportResult result = exporter.export(resolveFile(backup), DatabaseExporter.EXCLUDED_TABLES, actor);
			backup.setSizeBytes(result.sizeBytes())
				.setSha256(result.sha256())
				.setTableCount(result.tableCount())
				.setRowCount(result.rowCount())
				.setDatabaseProduct(result.databaseProduct());
			finish(backup, BackupStatus.COMPLETED, null);
			notifyOwner(userId, "Database backup completed", backup.getFileName() + " is ready to download.");
			if (isDriveUploadAutomatic(backup)) uploadToDrive(backup);
		} catch (Exception e) {
			log.error("[DatabaseBackup] backup {} failed: {}", backup.getFileName(), e.getMessage(), e);
			finish(backup, BackupStatus.FAILED, StringUtils.abbreviate(e.getMessage(), 2000));
			notifyOwner(userId, "Database backup failed", backup.getFileName() + " could not be created. See the backup list for details.");
		}
	}

	private void runScheduled(Long id, int keepCount) {
		runBackup(id, "system", null);
		DatabaseBackup done = backupRepo.findById(id).orElse(null);
		if (done == null) return;
		if (done.getStatus() == BackupStatus.FAILED) {
			try {
				notificationPublisher.notifyAuthority("backup:write", NotificationType.ADMIN_MESSAGE, ACTION_ROUTE,
					"title", "Scheduled database backup failed", "message", done.getFileName() + ": " + done.getErrorMessage());
			} catch (Exception e) {
				log.warn("[DatabaseBackup] could not send scheduled-backup failure notification: {}", e.getMessage());
			}
			return;
		}
		deleteOldScheduled(Math.max(1, keepCount));
	}

	private void deleteOldScheduled(int keepCount) {
		backupRepo.findAllByFileNameStartingWithAndStatusOrderByIdDesc(SCHEDULED_PREFIX + "-", BackupStatus.COMPLETED).stream().skip(keepCount).forEach(old -> {
			try {
				Files.deleteIfExists(resolveFile(old));
				deleteDriveCopy(old);
				backupRepo.delete(old);
				log.info("[DatabaseBackup] deleted old scheduled backup {}", old.getFileName());
			} catch (Exception e) {
				log.warn("[DatabaseBackup] could not delete old scheduled backup {}: {}", old.getFileName(), e.getMessage());
			}
		});
	}

	private boolean isDriveUploadAutomatic(DatabaseBackup backup) {
		boolean eligible = backup.getFileName().startsWith("db-backup-") || backup.getFileName().startsWith(SCHEDULED_PREFIX + "-");
		SystemConfig switchConfig = systemConfigService.findCachedByKey("DB_BACKUP_DRIVE_ENABLED");
		boolean enabled = switchConfig != null && "Y".equalsIgnoreCase(switchConfig.getConfigValue());
		return eligible && enabled && driveClient.isConfigured();
	}

	private void uploadToDrive(DatabaseBackup backup) {
		backup.setDriveStatus(DRIVE_UPLOADING).setDriveError(null);
		backup.setUpdatedOn(new Date());
		backupRepo.save(backup);
		try {
			String fileId = driveClient.upload(resolveFile(backup), backup.getFileName());
			backup.setDriveFileId(fileId).setDriveStatus(DRIVE_UPLOADED);
			log.info("[DatabaseBackup] uploaded {} to Google Drive", backup.getFileName());
		} catch (Exception e) {
			log.error("[DatabaseBackup] Google Drive upload of {} failed: {}", backup.getFileName(), e.getMessage(), e);
			backup.setDriveStatus(DRIVE_FAILED).setDriveError(StringUtils.abbreviate(e.getMessage(), 600));
			try {
				notificationPublisher.notifyAuthority("backup:write", NotificationType.ADMIN_MESSAGE, ACTION_ROUTE,
					"title", "Google Drive backup upload failed", "message", backup.getFileName() + ": " + backup.getDriveError());
			} catch (Exception notifyEx) {
				log.warn("[DatabaseBackup] could not send Drive failure notification: {}", notifyEx.getMessage());
			}
		}
		backup.setUpdatedOn(new Date());
		backupRepo.save(backup);
	}

	private void deleteDriveCopy(DatabaseBackup backup) {
		if (backup.getDriveFileId() == null || !driveClient.isConfigured()) return;
		try {
			driveClient.delete(backup.getDriveFileId());
		} catch (Exception e) {
			log.warn("[DatabaseBackup] could not delete the Drive copy of {}: {}", backup.getFileName(), e.getMessage());
		}
	}

	private void finish(DatabaseBackup backup, BackupStatus status, String error) {
		backup.setStatus(status).setErrorMessage(error).setFinishedOn(new Date());
		backup.setUpdatedOn(new Date());
		backupRepo.save(backup);
	}

	private void notifyOwner(Long userId, String title, String message) {
		if (userId == null) return;
		try {
			notificationPublisher.notifyUser(userId, NotificationType.ADMIN_MESSAGE, ACTION_ROUTE, "title", title, "message", message);
		} catch (Exception e) {
			log.warn("[DatabaseBackup] could not send backup notification: {}", e.getMessage());
		}
	}

	private DatabaseBackup findBackup(Long id) {
		return backupRepo.findById(id).orElseThrow(() -> new com.bd.erecruitment.exception.NotFoundException("Backup not found"));
	}

	private Path resolveFile(DatabaseBackup backup) {
		Path file = backupDir.resolve(backup.getFileName()).normalize();
		if (!file.startsWith(backupDir)) returnErrorException("Invalid backup file name");
		return file;
	}

	private void deleteLeftoverPartFiles() {
		if (!Files.isDirectory(backupDir)) return;
		try (DirectoryStream<Path> parts = Files.newDirectoryStream(backupDir, "*.part")) {
			for (Path part : parts) Files.deleteIfExists(part);
		} catch (IOException e) {
			log.warn("[DatabaseBackup] could not clean leftover .part files: {}", e.getMessage());
		}
	}
}
