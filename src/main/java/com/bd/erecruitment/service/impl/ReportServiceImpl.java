package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.report.ApplicationReportRow;
import com.bd.erecruitment.dto.report.AuditLogReportRow;
import com.bd.erecruitment.dto.report.JobPostingReportRow;
import com.bd.erecruitment.dto.report.McqResultReportRow;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.specification.GenericSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// One .jrxml template + row-building method per report key.
@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

	// Used when the caller sends no (or an unrecognised) time zone.
	private static final ZoneId DEFAULT_REPORT_ZONE = ZoneId.of("Asia/Dhaka");
	private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneOffset.UTC);
	private static final List<String> REPORT_KEYS = List.of("job-posting", "application", "mcq-result", "audit-log");

	private final JobCircularRepo jobCircularRepo;
	private final ApplicationRepo applicationRepo;
	private final McqTestAssignmentRepo mcqTestAssignmentRepo;
	private final McqTestRepo mcqTestRepo;
	private final AuditLogRepo auditLogRepo;
	private final UserRepo userRepo;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	// Compiled once per report key, reused across requests.
	private final Map<String, JasperReport> compiledReports = new ConcurrentHashMap<>();

	public byte[] generate(String reportKey, String format, Map<String, String> filters, String timeZone) {
		if (!REPORT_KEYS.contains(reportKey)) throw new BadRequestException("Unknown report: " + reportKey);

		List<?> rows = switch (reportKey) {
			case "job-posting" -> buildJobPostingRows(filters);
			case "application" -> buildApplicationRows(filters);
			case "mcq-result" -> buildMcqResultRows(filters);
			case "audit-log" -> buildAuditLogRows(filters);
			default -> throw new BadRequestException("Unknown report: " + reportKey);
		};

		try {
			Map<String, Object> params = new HashMap<>();
			params.put("frontendBaseUrl", frontendBaseUrl);
			// Dates render in the caller's zone with English AM/PM regardless of JVM default zone/locale (storage stays UTC).
			params.put(JRParameter.REPORT_TIME_ZONE, TimeZone.getTimeZone(resolveZone(timeZone)));
			params.put(JRParameter.REPORT_LOCALE, Locale.ENGLISH);

			JasperPrint print = JasperFillManager.fillReport(
				compiledReport(reportKey), params, new JRBeanCollectionDataSource(rows));
			return "XLSX".equalsIgnoreCase(format) ? exportXlsx(print) : JasperExportManager.exportReportToPdf(print);
		} catch (JRException e) {
			throw new IllegalStateException("Failed to generate report: " + reportKey, e);
		}
	}

	// DATE columns are stored as UTC midnight, so the calendar date is read in UTC.
	private String deadlineText(Date deadline) {
		return deadline == null ? "" : DEADLINE_FORMAT.format(Instant.ofEpochMilli(deadline.getTime()));
	}

	private ZoneId resolveZone(String timeZone) {
		if (timeZone == null || timeZone.isBlank()) return DEFAULT_REPORT_ZONE;
		try {
			return ZoneId.of(timeZone.trim());
		} catch (DateTimeException e) {
			return DEFAULT_REPORT_ZONE;
		}
	}

	private JasperReport compiledReport(String reportKey) {
		return compiledReports.computeIfAbsent(reportKey, key -> {
			try (InputStream is = getClass().getResourceAsStream("/reports/" + key + "-report.jrxml")) {
				if (is == null) throw new IllegalStateException("Report template not found: " + key);
				return JasperCompileManager.compileReport(is);
			} catch (JRException | java.io.IOException e) {
				throw new IllegalStateException("Failed to compile report template: " + key, e);
			}
		});
	}

	private byte[] exportXlsx(JasperPrint print) throws JRException {
		JRXlsxExporter exporter = new JRXlsxExporter();
		exporter.setExporterInput(SimpleExporterInput.getInstance(List.of(print)));
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(os));

		SimpleXlsxReportConfiguration config = new SimpleXlsxReportConfiguration();
		config.setOnePagePerSheet(false);
		config.setDetectCellType(true);
		config.setRemoveEmptySpaceBetweenRows(true);
		config.setWhitePageBackground(false);
		exporter.setConfiguration(config);

		exporter.exportReport();
		return os.toByteArray();
	}

	private List<JobPostingReportRow> buildJobPostingRows(Map<String, String> filters) {
		List<JobCircular> jobs = jobCircularRepo.findAll(GenericSpecification.<JobCircular>build(filters));
		if (jobs.isEmpty()) return List.of();

		List<Long> jobIds = jobs.stream().map(JobCircular::getId).toList();
		Map<Long, Long> applicantCounts = applicationRepo.countGroupByJobCircularIdIn(jobIds).stream()
			.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

		return jobs.stream()
			.map(job -> new JobPostingReportRow(
				job.getJobTitle(), job.getCompanyName(), job.getStatus(), job.getVacancy(),
				deadlineText(job.getApplicationDeadLine()), job.getJobLocation(), job.getEmploymentStatus(),
				applicantCounts.getOrDefault(job.getId(), 0L).intValue()
			))
			.toList();
	}

	private List<ApplicationReportRow> buildApplicationRows(Map<String, String> filters) {
		List<Application> applications = applicationRepo.findAll(GenericSpecification.<Application>build(filters));
		if (applications.isEmpty()) return List.of();

		Map<Long, String> jobTitles = jobTitlesByJobCircularId(applications.stream().map(Application::getJobCircularId).distinct().toList());
		Map<Long, User> candidates = usersByCandidateId(applications.stream().map(Application::getCandidateUserId).distinct().toList());

		return applications.stream()
			.map(application -> {
				String jobTitle = jobTitles.getOrDefault(application.getJobCircularId(), "");
				User candidate = candidates.get(application.getCandidateUserId());
				return new ApplicationReportRow(
					jobTitle,
					candidate != null ? candidate.getFullName() : "",
					candidate != null ? candidate.getEmail() : "",
					application.getStatus(), application.getAppliedOn()
				);
			})
			.toList();
	}

	// Skips ungraded assignments (no score yet).
	private List<McqResultReportRow> buildMcqResultRows(Map<String, String> filters) {
		List<McqTestAssignment> assignments = mcqTestAssignmentRepo.findAll(GenericSpecification.<McqTestAssignment>build(filters)).stream()
			.filter(a -> a.getScorePercent() != null)
			.toList();
		if (assignments.isEmpty()) return List.of();

		Map<Long, String> testNames = mcqTestRepo.findAllByIdInAndDeleted(assignments.stream().map(McqTestAssignment::getMcqTestId).distinct().toList(), false)
			.stream().collect(Collectors.toMap(McqTest::getId, McqTest::getName));
		Map<Long, User> candidates = usersByCandidateId(assignments.stream().map(McqTestAssignment::getCandidateUserId).distinct().toList());
		Map<Long, Application> applications = applicationRepo.findAllByIdInAndDeleted(assignments.stream().map(McqTestAssignment::getApplicationId).distinct().toList(), false)
			.stream().collect(Collectors.toMap(Application::getId, a -> a));
		Map<Long, String> jobTitles = jobTitlesByJobCircularId(applications.values().stream().map(Application::getJobCircularId).distinct().toList());

		return assignments.stream()
			.map(assignment -> {
				String testName = testNames.getOrDefault(assignment.getMcqTestId(), "");
				User candidate = candidates.get(assignment.getCandidateUserId());
				Application application = applications.get(assignment.getApplicationId());
				String jobTitle = application != null ? jobTitles.getOrDefault(application.getJobCircularId(), "") : "";

				return new McqResultReportRow(
					testName,
					candidate != null ? candidate.getFullName() : "",
					candidate != null ? candidate.getEmail() : "",
					jobTitle, assignment.getScorePercent(),
					Boolean.TRUE.equals(assignment.getPassed()) ? "Yes" : "No",
					assignment.getSubmittedOn()
				);
			})
			.toList();
	}

	private List<AuditLogReportRow> buildAuditLogRows(Map<String, String> filters) {
		return auditLogRepo.findAll(GenericSpecification.<AuditLog>build(filters)).stream()
			.map(log -> new AuditLogReportRow(
				log.getCategory() != null ? log.getCategory().name() : "",
				log.getAction(), log.getEntityType(), log.getEntityId(),
				log.getOutcome() != null ? log.getOutcome().name() : "",
				log.getCreatedBy(), log.getCreatedOn()
			))
			.toList();
	}

	private Map<Long, String> jobTitlesByJobCircularId(List<Long> jobCircularIds) {
		if (jobCircularIds.isEmpty()) return Map.of();
		return jobCircularRepo.findAllByIdInAndDeleted(jobCircularIds, false).stream()
			.collect(Collectors.toMap(JobCircular::getId, JobCircular::getJobTitle));
	}

	private Map<Long, User> usersByCandidateId(List<Long> userIds) {
		if (userIds.isEmpty()) return Map.of();
		return userRepo.findAllByIdInAndDeleted(userIds, false).stream()
			.collect(Collectors.toMap(User::getId, u -> u));
	}
}
