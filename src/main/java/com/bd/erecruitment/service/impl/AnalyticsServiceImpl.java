package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.ApplicationFunnelResDTO;
import com.bd.erecruitment.dto.res.RecruitmentSummaryResDTO;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.ApplicationStatusHistoryRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.util.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl {

	private static final String STAFF_AUTHORITY = "job-circular:write";

	private final ApplicationRepo applicationRepo;
	private final ApplicationStatusHistoryRepo historyRepo;
	private final JobCircularRepo jobCircularRepo;

	public Response<RecruitmentSummaryResDTO> summary() {
		requireStaff();

		RecruitmentSummaryResDTO dto = new RecruitmentSummaryResDTO();
		dto.setTotalJobs(jobCircularRepo.countByDeleted(false));
		dto.setPublishedJobs(jobCircularRepo.countByStatusAndDeleted("PUBLISHED", false));
		dto.setTotalApplications(applicationRepo.countByDeleted(false));

		Date thirtyDaysAgo = daysAgo(30);
		dto.setApplicationsLast30Days(applicationRepo.countByDeletedAndAppliedOnAfter(false, thirtyDaysAgo));

		Map<String, Long> byStatus = new HashMap<>();
		for (Object[] row : applicationRepo.countGroupByStatus()) {
			byStatus.put((String) row[0], (Long) row[1]);
		}
		dto.setApplicationsByStatus(byStatus);

		dto.setHiresLast30Days(historyRepo.countByStatusAndDeletedAndChangedOnAfter("HIRED", false, thirtyDaysAgo));
		dto.setAvgTimeToHireDays(computeAvgTimeToHire(historyRepo.findChangedOnAndAppliedOnByStatus("HIRED")));

		return success(dto);
	}

	public Response<ApplicationFunnelResDTO> funnel(Long jobCircularId) {
		requireStaff();

		List<Application> applications = jobCircularId != null
			? applicationRepo.findAllByJobCircularIdAndDeleted(jobCircularId, false)
			: applicationRepo.findAllByDeleted(false);

		Map<String, Long> counts = new HashMap<>();
		for (Application application : applications) {
			counts.merge(application.getStatus(), 1L, Long::sum);
		}

		ApplicationFunnelResDTO dto = new ApplicationFunnelResDTO();
		dto.setJobCircularId(jobCircularId);
		dto.setTotalApplications(applications.size());
		dto.setStatusCounts(counts);
		if (jobCircularId != null) {
			jobCircularRepo.findByIdAndDeleted(jobCircularId, false).ifPresent(job -> dto.setJobTitle(job.getJobTitle()));
		}

		Response<ApplicationFunnelResDTO> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage("Found");
		response.setObj(dto);
		return response;
	}

	private Double computeAvgTimeToHire(List<Object[]> changedOnAndAppliedOn) {
		if (changedOnAndAppliedOn.isEmpty()) return null;

		double totalDays = 0;
		for (Object[] row : changedOnAndAppliedOn) {
			long diffMs = ((Date) row[0]).getTime() - ((Date) row[1]).getTime();
			totalDays += diffMs / (1000.0 * 60 * 60 * 24);
		}
		return totalDays / changedOnAndAppliedOn.size();
	}

	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -days);
		return cal.getTime();
	}

	private void requireStaff() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof MyUserDetail me)) {
			throw new ForbiddenException("Access denied");
		}
		boolean staff = me.getAuthorities().stream().anyMatch(a ->
			STAFF_AUTHORITY.equals(a.getAuthority()) || "SUPER_ADMIN".equals(a.getAuthority()));
		if (!staff) throw new ForbiddenException("Only recruiters/admins may view analytics");
	}

	private <R> Response<R> success(R obj) {
		Response<R> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage("Found");
		response.setObj(obj);
		return response;
	}
}
