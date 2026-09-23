package com.bd.erecruitment.retention;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.exception.ExceptionLogWriter;
import com.bd.erecruitment.service.impl.ArchiveConfigServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

// Single scheduled entry point for every table's retention archiving, driven by ArchiveConfig rows.
@Slf4j
@Component
public class ArchiveScheduler {

	private final ArchiveConfigServiceImpl archiveConfigService;
	private final GenericArchiveEngine archiveEngine;
	private final ExceptionLogWriter exceptionLogWriter;

	public ArchiveScheduler(ArchiveConfigServiceImpl archiveConfigService, GenericArchiveEngine archiveEngine, ExceptionLogWriter exceptionLogWriter) {
		this.archiveConfigService = archiveConfigService;
		this.archiveEngine = archiveEngine;
		this.exceptionLogWriter = exceptionLogWriter;
	}

	@Scheduled(cron = "0 0 3 * * *")
	public void runScheduledArchiving() {
		List<ArchiveConfig> configs = archiveConfigService.findEnabled();
		for (ArchiveConfig config : configs) {
			try {
				int archived = archiveEngine.archive(config);
				if (archived > 0) log.info("[ArchiveScheduler] {}: archived and purged {} row(s)", config.getSourceTable(), archived);
			} catch (Exception ex) {
				// One misconfigured/missing archive table must not block the rest of this run.
				log.error("[ArchiveScheduler] {}: archiving failed: {}", config.getSourceTable(), ex.getMessage(), ex);
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "ArchiveScheduler:" + config.getSourceTable());
			}
		}
	}
}
