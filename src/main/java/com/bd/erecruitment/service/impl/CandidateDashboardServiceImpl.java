package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.CandidateDashboardResDTO;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.JobAlertRepo;
import com.bd.erecruitment.repository.OfferRepo;
import com.bd.erecruitment.repository.SavedJobRepo;
import com.bd.erecruitment.util.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CandidateDashboardServiceImpl extends CommonFunctionsImpl {

	private static final Set<String> ACTIVE_STATUSES = Set.of("APPLIED", "SCREENING", "INTERVIEW", "OFFER");
	private static final int RECENT_LIMIT = 5;

	private final ApplicationRepo applicationRepo;
	private final OfferRepo offerRepo;
	private final SavedJobRepo savedJobRepo;
	private final JobAlertRepo jobAlertRepo;

	public Response<CandidateDashboardResDTO> summary() {
		Long userId = getLoggedInUserDetails().getId();

		CandidateDashboardResDTO dto = new CandidateDashboardResDTO();
		for (Object[] row : applicationRepo.countGroupByStatusForCandidate(userId)) {
			long count = (Long) row[1];
			dto.setTotalApplications(dto.getTotalApplications() + count);
			if (ACTIVE_STATUSES.contains((String) row[0])) dto.setActiveApplications(dto.getActiveApplications() + count);
		}
		dto.setOffersToRespond(offerRepo.countSentByCandidateUserId(userId));
		dto.setSavedJobs(savedJobRepo.countByUserIdAndDeleted(userId, false));
		dto.setJobAlerts(jobAlertRepo.countByUserIdAndDeleted(userId, false));
		dto.setRecentApplications(applicationRepo.findRecentForCandidate(userId, PageRequest.ofSize(RECENT_LIMIT)).stream().map(r -> {
			CandidateDashboardResDTO.RecentApplication recent = new CandidateDashboardResDTO.RecentApplication();
			recent.setId((Long) r[0]);
			recent.setJobTitle((String) r[1]);
			recent.setStatus((String) r[2]);
			recent.setAppliedOn((Date) r[3]);
			return recent;
		}).toList());

		return getSuccessResponse("Found", dto);
	}
}
