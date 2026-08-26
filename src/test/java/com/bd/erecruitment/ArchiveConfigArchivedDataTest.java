package com.bd.erecruitment;

import com.bd.erecruitment.dto.res.ArchivedDataResDTO;
import com.bd.erecruitment.entity.ExceptionLog;
import com.bd.erecruitment.repository.ExceptionLogRepo;
import com.bd.erecruitment.service.impl.ArchiveConfigServiceImpl;
import com.bd.erecruitment.util.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Calendar;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

// Proves findArchivedData reads back rows GenericArchiveEngine already moved, with working pagination.
@SpringBootTest
class ArchiveConfigArchivedDataTest {

	@Autowired
	private ExceptionLogRepo exceptionLogRepo;

	@Autowired
	private com.bd.erecruitment.repository.ArchiveConfigRepo archiveConfigRepo;

	@Autowired
	private ArchiveConfigServiceImpl archiveConfigService;

	@Test
	void findArchivedData_returnsPreviouslyArchivedRowsWithColumnsAndPaging() {
		ExceptionLog first = saveRow(daysAgo(200), "first-trace");
		ExceptionLog second = saveRow(daysAgo(200), "second-trace");
		Long configId = archiveConfigRepo.findBySourceTableAndDeleted("EXCEPTION_LOG", false).orElseThrow().getId();
		archiveConfigService.archiveNow(configId);

		Response<ArchivedDataResDTO> page1 = archiveConfigService.findArchivedData(configId, 0, 1);
		ArchivedDataResDTO dto = page1.getObj();

		assertThat(dto.getColumns()).contains("TRACE_ID", "EXCEPTION_CLASS", "ID");
		assertThat(dto.getTotalElements()).isGreaterThanOrEqualTo(2);
		assertThat(dto.getRows()).hasSize(1);

		Response<ArchivedDataResDTO> page2 = archiveConfigService.findArchivedData(configId, 1, 1);
		assertThat(page2.getObj().getRows()).hasSize(1);
		assertThat(page1.getObj().getRows().get(0)).isNotEqualTo(page2.getObj().getRows().get(0));
	}

	private ExceptionLog saveRow(Date createdOn, String traceId) {
		ExceptionLog entry = new ExceptionLog()
				.setTraceId(traceId)
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
