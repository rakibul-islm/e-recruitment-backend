package com.bd.erecruitment.seed.data;

import java.util.List;

public class SystemConfigData {

	public record ConfigDef(String key, String value, String description, String expectedValues) {}

	public static List<ConfigDef> get() {
		return List.of(
			new ConfigDef(
				"EXCEPTION_LOG_TO_DB",
				"Y",
				"When Y, exceptions handled by GlobalExceptionHandler are also persisted to the exception_log table",
				"Y,N"
			)
		);
	}
}
