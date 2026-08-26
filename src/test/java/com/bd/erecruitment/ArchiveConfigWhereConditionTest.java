package com.bd.erecruitment;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.retention.GenericArchiveEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Proves the optional where condition further narrows which past-retention rows get archived, and that unsafe text is rejected.
@SpringBootTest
class ArchiveConfigWhereConditionTest {

	@Autowired
	private ExceptionLogRepo exceptionLogRepo;

	@Autowired
	private ArchiveConfigRepo archiveConfigRepo;

	@Autowired
	private GenericArchiveEngine archiveEngine;

	@Test
	void archive_onlyMovesRowsMatchingTheExtraCondition() {
		ExceptionLog matching = saveRow(daysAgo(200), "com.bd.erecruitment.exception.TargetException");
		ExceptionLog nonMatching = saveRow(daysAgo(200), "java.lang.RuntimeException");

		ArchiveConfig config = archiveConfigRepo.findBySourceTableAndDeleted("EXCEPTION_LOG", false).orElseThrow();
		config.setWhereCondition("exception_class = 'com.bd.erecruitment.exception.TargetException'");
		archiveConfigRepo.save(config);

		archiveEngine.archive(config);

		assertThat(exceptionLogRepo.findById(matching.getId())).isEmpty();
		assertThat(exceptionLogRepo.findById(nonMatching.getId())).isPresent();

		config.setWhereCondition(null);
		archiveConfigRepo.save(config);
	}

	@Test
	void archive_rejectsAConditionContainingAStatementSeparator() {
		ArchiveConfig config = archiveConfigRepo.findBySourceTableAndDeleted("EXCEPTION_LOG", false).orElseThrow();
		config.setWhereCondition("1=1; DROP TABLE exception_log");
		archiveConfigRepo.save(config);

		assertThatThrownBy(() -> archiveEngine.archive(config)).isInstanceOf(IllegalArgumentException.class);

		config.setWhereCondition(null);
		archiveConfigRepo.save(config);
	}

	private ExceptionLog saveRow(Date createdOn, String exceptionClass) {
		ExceptionLog entry = new ExceptionLog()
				.setTraceId("trace-1")
				.setExceptionClass(exceptionClass)
				.setStatusCode(500)
				.setRequestUri("/api/test")
				.setMessage("boom");
		entry.setCreatedBy("system").setCreatedOn(createdOn)
				.setUpdatedBy("system").setUpdatedOn(createdOn)
				.setDeleted(false);
		return exceptionLogRepo.save(entry);
	}

	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -days);
		return cal.getTime();
	}
}
