package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.report.ApplicationReportRow;
import com.bd.erecruitment.dto.report.AuditLogReportRow;
import com.bd.erecruitment.dto.report.JobPostingReportRow;
import com.bd.erecruitment.dto.report.McqResultReportRow;
import com.bd.erecruitment.dto.report.McqViolationReportRow;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.entity.McqTestViolation;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.AuditLogRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.McqTestViolationRepo;
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
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl extends CommonFunctionsImpl {

	private static final ZoneId DEFAULT_REPORT_ZONE = ZoneId.of("Asia/Dhaka");
	private static final DateTimeFormatter DEADLINE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy").withZone(ZoneOffset.UTC);
	private static final List<String> REPORT_KEYS = List.of("job-posting", "application", "mcq-result", "audit-log", "mcq-violation");

	private final JobCircularRepo jobCircularRepo;
	private final ApplicationRepo applicationRepo;
	private final McqTestAssignmentRepo mcqTestAssignmentRepo;
	private final McqTestRepo mcqTestRepo;
	private final McqTestViolationRepo mcqTestViolationRepo;
	private final AuditLogRepo auditLogRepo;
	private final UserRepo userRepo;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	private final Map<String, JasperReport> compiledReports = new ConcurrentHashMap<>();

	public byte[] generate(String reportKey, String format, Map<String, String> filters, String timeZone) {
		if (!REPORT_KEYS.contains(reportKey)) throw new BadRequestException("Unknown report: " + reportKey);

		List<?> rows = switch (reportKey) {
			case "job-posting" -> buildJobPostingRows(filters);
			case "application" -> buildApplicationRows(filters);
			case "mcq-result" -> buildMcqResultRows(filters);
			case "audit-log" -> buildAuditLogRows(filters);
			case "mcq-violation" -> buildMcqViolationRows(filters);
			default -> throw new BadRequestException("Unknown report: " + reportKey);
		};

		try {
			Map<String, Object> params = new HashMap<>();
			params.put("frontendBaseUrl", frontendBaseUrl);
			params.put(JRParameter.REPORT_TIME_ZONE, TimeZone.getTimeZone(resolveZone(timeZone)));
			params.put(JRParameter.REPORT_LOCALE, Locale.ENGLISH);

			JasperPrint print = JasperFillManager.fillReport(
				compiledReport(reportKey), params, new JRBeanCollectionDataSource(rows));
			return "XLSX".equalsIgnoreCase(format) ? exportXlsx(print) : JasperExportManager.exportReportToPdf(print);
		} catch (JRException e) {
			throw new IllegalStateException("Failed to generate report: " + reportKey, e);
		}
	}

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
		Long organizationId = organizationScope();
		if (organizationId != null) {
			filters = new HashMap<>(filters);
			filters.put("organizationId", String.valueOf(organizationId));
		}
		List<JobCircular> jobs = jobCircularRepo.findAll(GenericSpecification.<JobCircular>build(filters));
		if (jobs.isEmpty()) return List.of();

		List<Long> jobIds = jobs.stream().map(JobCircular::getId).toList();
		Map<Long, Long> applicantCounts = applicationRepo.countGroupByJobCircularIdIn(jobIds).stream()
			.collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

		return jobs.stream()
			.map(job -> new JobPostingReportRow(
				job.getJobTitle(), job.getOrganizationName(), job.getStatus(), job.getVacancy(),
				deadlineText(job.getApplicationDeadLine()), job.getJobLocation(), job.getEmploymentStatus(),
				applicantCounts.getOrDefault(job.getId(), 0L).intValue(), job.getCategory()
			))
			.toList();
	}

	private List<ApplicationReportRow> buildApplicationRows(Map<String, String> filters) {
		List<Application> applications = applicationRepo.findAll(GenericSpecification.<Application>build(resolveJobTitleFilter(resolveCandidateFilters(filters))));
		Long organizationId = organizationScope();
		if (organizationId != null) {
			Set<Long> ownJobIds = ownJobIds(organizationId);
			applications = applications.stream().filter(a -> ownJobIds.contains(a.getJobCircularId())).toList();
		}
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
					application.getStatus(), application.getAppliedOn(), application.getMatchScore()
				);
			})
			.toList();
	}

	private List<McqResultReportRow> buildMcqResultRows(Map<String, String> filters) {
		List<McqTestAssignment> assignments = mcqTestAssignmentRepo.findAll(GenericSpecification.<McqTestAssignment>build(resolveCandidateFilters(filters))).stream()
			.filter(a -> a.getScorePercent() != null)
			.toList();
		Long organizationId = organizationScope();
		if (organizationId != null) {
			Set<Long> ownTestIds = ownTestIds(organizationId);
			assignments = assignments.stream().filter(a -> ownTestIds.contains(a.getMcqTestId())).toList();
		}
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

	private List<McqViolationReportRow> buildMcqViolationRows(Map<String, String> filters) {
		List<McqTestViolation> violations = mcqTestViolationRepo.findAll(GenericSpecification.<McqTestViolation>build(resolveCandidateFilters(filters))).stream()
			.sorted(Comparator.comparing(McqTestViolation::getCreatedOn).reversed())
			.toList();
		Long organizationId = organizationScope();
		if (organizationId != null) {
			Set<Long> ownTestIds = ownTestIds(organizationId);
			violations = violations.stream().filter(v -> ownTestIds.contains(v.getMcqTestId())).toList();
		}
		if (violations.isEmpty()) return List.of();

		Map<Long, String> testNames = mcqTestRepo.findAllByIdInAndDeleted(violations.stream().map(McqTestViolation::getMcqTestId).distinct().toList(), false)
			.stream().collect(Collectors.toMap(McqTest::getId, McqTest::getName));
		Map<Long, User> candidates = usersByCandidateId(violations.stream().map(McqTestViolation::getCandidateUserId).distinct().toList());
		Map<Long, McqTestAssignment> assignments = mcqTestAssignmentRepo.findAllByIdInAndDeleted(violations.stream().map(McqTestViolation::getAssignmentId).distinct().toList(), false)
			.stream().collect(Collectors.toMap(McqTestAssignment::getId, a -> a));
		Map<Long, Application> applications = applicationRepo.findAllByIdInAndDeleted(assignments.values().stream().map(McqTestAssignment::getApplicationId).distinct().toList(), false)
			.stream().collect(Collectors.toMap(Application::getId, a -> a));
		Map<Long, String> jobTitles = jobTitlesByJobCircularId(applications.values().stream().map(Application::getJobCircularId).distinct().toList());

		return violations.stream()
			.map(violation -> {
				User candidate = candidates.get(violation.getCandidateUserId());
				McqTestAssignment assignment = assignments.get(violation.getAssignmentId());
				Application application = assignment != null ? applications.get(assignment.getApplicationId()) : null;
				String jobTitle = application != null ? jobTitles.getOrDefault(application.getJobCircularId(), "") : "";

				return new McqViolationReportRow(
					testNames.getOrDefault(violation.getMcqTestId(), ""),
					candidate != null ? candidate.getFullName() : "",
					candidate != null ? candidate.getEmail() : "",
					jobTitle, violation.getViolationType(), violation.getDetail(),
					violation.getQuestionNumber(), violation.getSequenceNo(), violation.getAction(), violation.getCreatedOn()
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
				log.getCreatedBy(), log.getCreatedOn(),
				log.getIpAddress(), log.getCity(), log.getCountry(), log.getDeviceType(), log.getOs(), log.getBrowser()
			))
			.toList();
	}

	// Candidate name/email are not columns on these tables, so they are resolved to candidate ids first.
	private Map<String, String> resolveCandidateFilters(Map<String, String> filters) {
		String candidateName = filters.get("candidateName_like");
		String candidateEmail = filters.get("candidateEmail_like");
		boolean hasName = candidateName != null && !candidateName.isBlank();
		boolean hasEmail = candidateEmail != null && !candidateEmail.isBlank();

		Map<String, String> effectiveFilters = new HashMap<>(filters);
		effectiveFilters.remove("candidateName_like");
		effectiveFilters.remove("candidateEmail_like");
		if (!hasName && !hasEmail) return effectiveFilters;

		Map<String, String> userFilters = new HashMap<>();
		if (hasName) userFilters.put("fullName_like", candidateName);
		if (hasEmail) userFilters.put("email_like", candidateEmail);

		String ids = userRepo.findAll(GenericSpecification.<User>build(userFilters)).stream()
			.map(user -> String.valueOf(user.getId())).collect(Collectors.joining(","));
		effectiveFilters.put("candidateUserId_in", ids.isEmpty() ? "-1" : ids);
		return effectiveFilters;
	}

	private Map<String, String> resolveJobTitleFilter(Map<String, String> filters) {
		String jobTitle = filters.get("jobTitle_like");
		if (jobTitle == null || jobTitle.isBlank()) return filters;

		Map<String, String> effectiveFilters = new HashMap<>(filters);
		effectiveFilters.remove("jobTitle_like");
		String ids = jobCircularRepo.findAll(GenericSpecification.<JobCircular>build(Map.of("jobTitle_like", jobTitle))).stream()
			.map(job -> String.valueOf(job.getId())).collect(Collectors.joining(","));
		effectiveFilters.put("jobCircularId_in", ids.isEmpty() ? "-1" : ids);
		return effectiveFilters;
	}

	// Null for admins; a scoped recruiter without an organization gets -1 so nothing matches.
	private Long organizationScope() {
		if (!isScopedRecruiter()) return null;
		Long organizationId = getLoggedInUserDetails().getOrganizationId();
		return organizationId != null ? organizationId : -1L;
	}

	private Set<Long> ownJobIds(Long organizationId) {
		return jobCircularRepo.findAll(GenericSpecification.<JobCircular>build(Map.of("organizationId", String.valueOf(organizationId))))
			.stream().map(JobCircular::getId).collect(Collectors.toSet());
	}

	private Set<Long> ownTestIds(Long organizationId) {
		return mcqTestRepo.findAll(GenericSpecification.<McqTest>build(Map.of("organizationId", String.valueOf(organizationId))))
			.stream().map(McqTest::getId).collect(Collectors.toSet());
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
