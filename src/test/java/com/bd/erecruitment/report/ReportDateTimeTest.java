package com.bd.erecruitment.report;

import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTestViolation;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.McqTestViolationRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.impl.ReportServiceImpl;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
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
	private McqTestViolationRepo violationRepo;
	private ReportServiceImpl reportService;

	@BeforeEach
	void setUp() {
		originalZone = TimeZone.getDefault();
		auditLogRepo = mock(AuditLogRepo.class);
		jobCircularRepo = mock(JobCircularRepo.class);
		violationRepo = mock(McqTestViolationRepo.class);
		reportService = new ReportServiceImpl(jobCircularRepo, mock(ApplicationRepo.class),
				mock(McqTestAssignmentRepo.class), mock(McqTestRepo.class), violationRepo, auditLogRepo, mock(UserRepo.class));
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
		for (String key : List.of("job-posting", "application", "mcq-result", "audit-log", "mcq-violation")) {
			assertThat(pdfText(reportService.generate(key, "PDF", Map.of(), null))).as(key).isNotBlank();
		}
	}

	@Test
	void violationReportShowsEachEventInPdfAndKeepsExcelColumnsIntact() throws Exception {
		McqTestViolation violation = new McqTestViolation().setAssignmentId(1L).setMcqTestId(2L).setCandidateUserId(3L)
				.setViolationType("BLOCKED_SHORTCUT").setDetail("Ctrl+C").setQuestionNumber(4).setSequenceNo(2).setAction("WARNED");
		violation.setCreatedBy("system").setCreatedOn(Date.from(Instant.parse("2030-03-04T20:30:45Z")));
		when(violationRepo.findAll(any(Specification.class))).thenReturn(List.of(violation));

		String pdf = pdfText(reportService.generate("mcq-violation", "PDF", Map.of(), "Asia/Dhaka"));
		assertThat(pdf).contains("MCQ Test Violation Report").contains("BLOCKED_SHORTCUT").contains("Ctrl+C")
				.contains("WARNED").contains("05-03-2030 02:30:45 AM");

		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(
				reportService.generate("mcq-violation", "XLSX", Map.of(), "Asia/Dhaka")))) {
			Sheet sheet = workbook.getSheetAt(0);
			DataFormatter formatter = new DataFormatter();
			List<String> headerRow = null;
			List<String> dataRow = null;
			for (Row row : sheet) {
				List<String> cells = new ArrayList<>();
				for (Cell cell : row) {
					String text = formatter.formatCellValue(cell);
					if (!text.isBlank()) cells.add(text);
				}
				if (cells.contains("Occurred On")) headerRow = cells;
				if (cells.contains("BLOCKED_SHORTCUT")) dataRow = cells;
			}
			assertThat(headerRow).containsExactly("Occurred On", "Candidate", "Email", "Test", "Job", "Violation", "Detail", "Q#", "Seq", "Action");
			assertThat(dataRow).contains("05-03-2030 02:30:45 AM", "BLOCKED_SHORTCUT", "Ctrl+C", "4", "2", "WARNED");
			assertThat(widestTextCellSpan(sheet, headerRowIndex(sheet, formatter), formatter)).as("header columns").isEqualTo(1);
			assertThat(widestTextCellSpan(sheet, dataRowIndex(sheet, formatter), formatter)).as("data columns").isEqualTo(1);
		}
	}

	private int headerRowIndex(Sheet sheet, DataFormatter formatter) {
		return rowIndexWithText(sheet, formatter, "Occurred On");
	}

	private int dataRowIndex(Sheet sheet, DataFormatter formatter) {
		return rowIndexWithText(sheet, formatter, "BLOCKED_SHORTCUT");
	}

	private int rowIndexWithText(Sheet sheet, DataFormatter formatter, String text) {
		for (Row row : sheet) {
			for (Cell cell : row) if (text.equals(formatter.formatCellValue(cell))) return row.getRowNum();
		}
		throw new AssertionError(text + " not found");
	}

	private int widestTextCellSpan(Sheet sheet, int rowIndex, DataFormatter formatter) {
		int widest = 1;
		for (Cell cell : sheet.getRow(rowIndex)) {
			if (formatter.formatCellValue(cell).isBlank()) continue;
			for (CellRangeAddress region : sheet.getMergedRegions()) {
				if (region.isInRange(cell)) widest = Math.max(widest, region.getLastColumn() - region.getFirstColumn() + 1);
			}
		}
		return widest;
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
