package com.bd.erecruitment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

	/**
	 * Dedicated, bounded executor for audit-log persistence. Deliberately not the default
	 * (unbounded) @Async executor: a full queue falls back to CallerRunsPolicy (synchronous on
	 * the calling thread) instead of growing without limit or silently dropping audit events.
	 */
	@Bean(name = "auditLogExecutor")
	public Executor auditLogExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("audit-log-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}
}
