package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.AnalyticsListResDTO;
import com.bd.erecruitment.dto.res.ApplicationFunnelResDTO;
import com.bd.erecruitment.dto.res.RecruitmentSummaryResDTO;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.ApplicationStatusHistoryRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.util.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl extends CommonFunctionsImpl {

	private static final String STAFF_AUTHORITY = "job-circular:write";
	private static final double MILLIS_PER_DAY = 1000.0 * 60 * 60 * 24;

	private final ApplicationRepo applicationRepo;
	private final ApplicationStatusHistoryRepo historyRepo;
	private final JobCircularRepo jobCircularRepo;

	public Response<RecruitmentSummaryResDTO> summary() {
		requireStaff();

		Long organizationId = organizationScope();
		Date since = daysAgo(30);
		Map<String, Long> byStatus = statusCounts(null, organizationId);

		RecruitmentSummaryResDTO dto = new RecruitmentSummaryResDTO();
		dto.setTotalJobs(jobCircularRepo.countScoped(null, organizationId));
		dto.setPublishedJobs(jobCircularRepo.countScoped("PUBLISHED", organizationId));
		dto.setTotalApplications(byStatus.values().stream().mapToLong(Long::longValue).sum());
		dto.setApplicationsLast30Days(applicationRepo.countAppliedSince(since, organizationId));
		dto.setApplicationsByStatus(byStatus);
		// Hires and time to hire are app-wide, not limited to the recruiter's organization.
		dto.setHiresLast30Days(historyRepo.countApplicationsMovedToSince("HIRED", since, null));

		OptionalDouble avg = applicationRepo.findHiredDates(null).stream()
			.mapToDouble(r -> (((Date) r[0]).getTime() - ((Date) r[1]).getTime()) / MILLIS_PER_DAY).average();
		dto.setAvgTimeToHireDays(avg.isPresent() ? avg.getAsDouble() : null);

		return getSuccessResponse("Found", dto);
	}

	public Response<ApplicationFunnelResDTO> funnel(Long jobCircularId) {
		requireStaff();

		Long organizationId = organizationScope();
		Map<String, Long> counts = statusCounts(jobCircularId, organizationId);

		ApplicationFunnelResDTO dto = new ApplicationFunnelResDTO();
		dto.setJobCircularId(jobCircularId);
		dto.setTotalApplications(counts.values().stream().mapToLong(Long::longValue).sum());
		dto.setStatusCounts(counts);
		if (jobCircularId != null) {
			jobCircularRepo.findByIdAndDeleted(jobCircularId, false)
				.filter(job -> organizationId == null || organizationId.equals(job.getOrganizationId()))
				.ifPresent(job -> dto.setJobTitle(job.getJobTitle()));
		}

		return getSuccessResponse("Found", dto);
	}

	private Map<String, Long> statusCounts(Long jobCircularId, Long organizationId) {
		return applicationRepo.countGroupByStatus(jobCircularId, organizationId).stream()
			.collect(Collectors.toMap(r -> (String) r[0], r -> (Long) r[1]));
	}

	public Response<AnalyticsListResDTO> details(String mode, Pageable pageable) {
		requireStaff();

		Long organizationId = organizationScope();
		Page<AnalyticsListResDTO> page = switch (mode) {
			case "jobs" -> jobCircularRepo.findJobRows(organizationId, pageable).map(this::toJobRow);
			case "applications" -> applicationRows(null, organizationId, pageable);
			case "recent" -> applicationRows(daysAgo(30), organizationId, pageable);
			default -> throw new BadRequestException("Unknown analytics detail mode: " + mode);
		};

		return getSuccessResponse("Found", page);
	}

	// Null for admins; a scoped recruiter without an organization gets -1 so nothing matches.
	private Long organizationScope() {
		if (!isScopedRecruiter()) return null;
		Long organizationId = getLoggedInUserDetails().getOrganizationId();
		return organizationId != null ? organizationId : -1L;
	}

	private Page<AnalyticsListResDTO> applicationRows(Date appliedAfter, Long organizationId, Pageable pageable) {
		return applicationRepo.findDetailRows(appliedAfter, organizationId, pageable).map(this::toApplicationRow);
	}

	private AnalyticsListResDTO toJobRow(Object[] r) {
		AnalyticsListResDTO row = new AnalyticsListResDTO();
		row.setJobId((Long) r[0]);
		row.setJobTitle((String) r[1]);
		row.setStatus((String) r[2]);
		return row;
	}

	private AnalyticsListResDTO toApplicationRow(Object[] r) {
		AnalyticsListResDTO row = new AnalyticsListResDTO();
		row.setApplicationId((Long) r[0]);
		row.setJobId((Long) r[1]);
		row.setJobTitle((String) r[2]);
		row.setCandidateName((String) r[3]);
		row.setCandidateEmail((String) r[4]);
		row.setStatus((String) r[5]);
		row.setAppliedOn((Date) r[6]);
		return row;
	}

	// Start of day, so the summary count and the detail list use the same cutoff.
	private Date daysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_MONTH, -days);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	private void requireStaff() {
		MyUserDetail me = getLoggedInUserDetails();
		if (me == null) throw new ForbiddenException("Access denied");
		boolean staff = me.getAuthorities().stream().anyMatch(a ->
			STAFF_AUTHORITY.equals(a.getAuthority()) || "SUPER_ADMIN".equals(a.getAuthority()));
		if (!staff) throw new ForbiddenException("Only recruiters/admins may view analytics");
	}
}
