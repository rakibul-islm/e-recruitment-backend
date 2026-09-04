package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.res.ApplicationFunnelResDTO;
import com.bd.erecruitment.dto.res.RecruitmentSummaryResDTO;
import com.bd.erecruitment.service.impl.AnalyticsServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestApiController
@RequestMapping("/analytics")
@RequiredArgsConstructor
@Tag(name = "6.0 Analytics", description = "Recruitment KPIs (recruiter/admin)")
public class AnalyticsController {

	private final AnalyticsServiceImpl analyticsService;

	@Operation(summary = "Recruitment summary (job/application counts, time-to-hire)")
	@GetMapping("/summary")
	public Response<RecruitmentSummaryResDTO> summary() {
		return analyticsService.summary();
	}

	@Operation(summary = "Application funnel, overall or for one job posting")
	@GetMapping("/funnel")
	public Response<ApplicationFunnelResDTO> funnel(@RequestParam(required = false) Long jobCircularId) {
		return analyticsService.funnel(jobCircularId);
	}
}
