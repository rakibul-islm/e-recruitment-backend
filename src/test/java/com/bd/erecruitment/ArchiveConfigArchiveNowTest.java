package com.bd.erecruitment;

import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.service.impl.ArchiveConfigServiceImpl;
import com.bd.erecruitment.util.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// Proves the manual "archive now" trigger runs the same job as the scheduler, on demand, for one config.
@SpringBootTest
class ArchiveConfigArchiveNowTest {

	@Autowired
	private ExceptionLogRepo exceptionLogRepo;

	@Autowired
	private ArchiveConfigRepo archiveConfigRepo;

	@Autowired
	private ArchiveConfigServiceImpl archiveConfigService;

	@Test
	void archiveNow_movesEligibleRowsImmediately() {
		ExceptionLog stale = saveRow(daysAgo(200));
		Long configId = archiveConfigRepo.findBySourceTableAndDeleted("EXCEPTION_LOG", false)
				.orElseThrow().getId();

		Response<Integer> response = archiveConfigService.archiveNow(configId);

		assertThat(response.getObj()).isEqualTo(1);
		assertThat(exceptionLogRepo.findById(stale.getId())).isEmpty();
	}

	private ExceptionLog saveRow(Date createdOn) {
		ExceptionLog entry = new ExceptionLog()
				.setTraceId("trace-1")
				.setExceptionClass("java.lang.RuntimeException")
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
