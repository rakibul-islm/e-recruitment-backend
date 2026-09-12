package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.service.impl.ReportServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@RestApiController
@RequestMapping("/report")
@RequiredArgsConstructor
@Tag(name = "7.0 Report", description = "JasperReports-based downloadable reports (recruiter/admin)")
public class ReportController {

	private static final MediaType XLSX_MEDIA_TYPE =
		MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

	private final ReportServiceImpl reportService;

	@Operation(summary = "Generate a report as PDF or Excel (format=PDF|XLSX, remaining params are report-specific filters)")
	@GetMapping("/{reportKey}/generate")
	public ResponseEntity<byte[]> generate(@PathVariable String reportKey, @RequestParam Map<String, String> filters) {
		String format = filters.getOrDefault("format", "PDF");
		Map<String, String> dataFilters = new HashMap<>(filters);
		dataFilters.remove("format");

		byte[] bytes = reportService.generate(reportKey, format, dataFilters);
		boolean xlsx = "XLSX".equalsIgnoreCase(format);

		return ResponseEntity.ok()
			.contentType(xlsx ? XLSX_MEDIA_TYPE : MediaType.APPLICATION_PDF)
			.header(HttpHeaders.CONTENT_DISPOSITION,
				"attachment; filename=\"" + reportKey + "-report." + (xlsx ? "xlsx" : "pdf") + "\"")
			.body(bytes);
	}
}
