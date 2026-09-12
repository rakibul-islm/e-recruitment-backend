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
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// One .jrxml template + row-building method per report key.
@Service
@RequiredArgsConstructor
public class ReportServiceImpl {

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

	public byte[] generate(String reportKey, String format, Map<String, String> filters) {
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

			JasperPrint print = JasperFillManager.fillReport(
				compiledReport(reportKey), params, new JRBeanCollectionDataSource(rows));
			return "XLSX".equalsIgnoreCase(format) ? exportXlsx(print) : JasperExportManager.exportReportToPdf(print);
		} catch (JRException e) {
			throw new IllegalStateException("Failed to generate report: " + reportKey, e);
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
		return jobCircularRepo.findAll(GenericSpecification.<JobCircular>build(filters)).stream()
			.map(job -> new JobPostingReportRow(
				job.getJobTitle(), job.getCompanyName(), job.getStatus(), job.getVacancy(),
				job.getApplicationDeadLine(), job.getJobLocation(), job.getEmploymentStatus(),
				applicationRepo.findAllByJobCircularIdAndDeleted(job.getId(), false).size()
			))
			.toList();
	}

	private List<ApplicationReportRow> buildApplicationRows(Map<String, String> filters) {
		Map<Long, String> jobTitleCache = new HashMap<>();
		Map<Long, User> candidateCache = new HashMap<>();

		return applicationRepo.findAll(GenericSpecification.<Application>build(filters)).stream()
			.map(application -> {
				String jobTitle = jobTitleCache.computeIfAbsent(application.getJobCircularId(), this::jobTitleFor);
				User candidate = candidateCache.computeIfAbsent(application.getCandidateUserId(), this::userFor);
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
		Map<Long, String> testNameCache = new HashMap<>();
		Map<Long, User> candidateCache = new HashMap<>();
		Map<Long, Application> applicationCache = new HashMap<>();
		Map<Long, String> jobTitleCache = new HashMap<>();

		return mcqTestAssignmentRepo.findAll(GenericSpecification.<McqTestAssignment>build(filters)).stream()
			.filter(a -> a.getScorePercent() != null)
			.map(assignment -> {
				String testName = testNameCache.computeIfAbsent(assignment.getMcqTestId(),
					id -> mcqTestRepo.findByIdAndDeleted(id, false).map(McqTest::getName).orElse(""));
				User candidate = candidateCache.computeIfAbsent(assignment.getCandidateUserId(), this::userFor);
				Application application = applicationCache.computeIfAbsent(assignment.getApplicationId(),
					id -> applicationRepo.findByIdAndDeleted(id, false).orElse(null));
				String jobTitle = application != null
					? jobTitleCache.computeIfAbsent(application.getJobCircularId(), this::jobTitleFor)
					: "";

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

	private String jobTitleFor(Long jobCircularId) {
		return jobCircularRepo.findByIdAndDeleted(jobCircularId, false).map(JobCircular::getJobTitle).orElse("");
	}

	private User userFor(Long userId) {
		return userRepo.findByIdAndDeleted(userId, false).orElse(null);
	}
}
