package com.bd.erecruitment.report;

import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.impl.ReportServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportDateTimeTest {

	private TimeZone originalZone;
	private AuditLogRepo auditLogRepo;
	private JobCircularRepo jobCircularRepo;
	private ReportServiceImpl reportService;

	@BeforeEach
	void setUp() {
		originalZone = TimeZone.getDefault();
		auditLogRepo = mock(AuditLogRepo.class);
		jobCircularRepo = mock(JobCircularRepo.class);
		reportService = new ReportServiceImpl(jobCircularRepo, mock(ApplicationRepo.class),
				mock(McqTestAssignmentRepo.class), mock(McqTestRepo.class), auditLogRepo, mock(UserRepo.class));
		ReflectionTestUtils.setField(reportService, "frontendBaseUrl", "http://localhost");
	}

	@AfterEach
	void restoreZone() {
		TimeZone.setDefault(originalZone);
	}

	@Test
	void timestampsFollowTheZoneTheCallerSends() throws Exception {
		stubAuditLogAt("2030-03-04T20:30:45Z");

		assertThat(auditText("Asia/Dhaka")).contains("05-03-2030 02:30:45 AM").doesNotContain("20:30:45");
		assertThat(auditText("America/New_York")).contains("04-03-2030 03:30:45 PM").doesNotContain("20:30:45");
		assertThat(auditText("UTC")).contains("04-03-2030 08:30:45 PM");
	}

	@Test
	void missingOrUnrecognisedZoneFallsBackToBangladeshTime() throws Exception {
		stubAuditLogAt("2030-03-04T20:30:45Z");

		assertThat(auditText(null)).contains("05-03-2030 02:30:45 AM");
		assertThat(auditText("")).contains("05-03-2030 02:30:45 AM");
		assertThat(auditText("Not/AZone")).contains("05-03-2030 02:30:45 AM");
	}

	@Test
	void reportZoneDoesNotDependOnTheJvmDefaultZone() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Auckland"));
		stubAuditLogAt("2030-03-04T20:30:45Z");

		assertThat(auditText("Asia/Dhaka")).contains("05-03-2030 02:30:45 AM");
	}

	@Test
	void jobDeadlineIsACalendarDateAndDoesNotShiftWithTheViewerZone() throws Exception {
		JobCircular job = new JobCircular().setJobTitle("Deadline check")
				.setApplicationDeadLine(Date.from(Instant.parse("2030-01-15T00:00:00Z")));
		when(jobCircularRepo.findAll(any(Specification.class))).thenReturn(List.of(job));

		String text = pdfText(reportService.generate("job-posting", "PDF", Map.of(), "America/Los_Angeles"));

		assertThat(text).contains("15-01-2030").doesNotContain("14-01-2030");
	}

	@Test
	void everyReportTemplateStillCompilesAndRenders() throws Exception {
		for (String key : List.of("job-posting", "application", "mcq-result", "audit-log")) {
			assertThat(pdfText(reportService.generate(key, "PDF", Map.of(), null))).as(key).isNotBlank();
		}
	}

	private void stubAuditLogAt(String instant) {
		AuditLog log = new AuditLog().setCategory(AuditCategory.SECURITY).setAction("LOGIN").setEntityType("User")
				.setEntityId(7L).setOutcome(AuditOutcome.SUCCESS);
		log.setCreatedBy("system").setCreatedOn(Date.from(Instant.parse(instant)));
		when(auditLogRepo.findAll(any(Specification.class))).thenReturn(List.of(log));
	}

	private String auditText(String timeZone) throws Exception {
		return pdfText(reportService.generate("audit-log", "PDF", Map.of(), timeZone));
	}

	private String pdfText(byte[] pdf) throws Exception {
		try (PDDocument document = PDDocument.load(pdf)) {
			return new PDFTextStripper().getText(document);
		}
	}
}
