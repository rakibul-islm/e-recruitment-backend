package com.bd.erecruitment.retention;

import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.service.impl.McqTestAssignmentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

// Fallback for a candidate whose timer ran out without the client-side auto-submit firing (lost
// connection, closed tab). Every minute - not JobAlertScheduler's daily cadence - since exam
// deadlines need much finer granularity. Mirrors JobAlertScheduler's single-cron, per-row
// try/catch shape so one broken assignment doesn't block the rest of the sweep.
@Slf4j
@Component
@RequiredArgsConstructor
public class McqTestAssignmentExpirySweeper {

	private final McqTestAssignmentRepo mcqTestAssignmentRepo;
	private final McqTestAssignmentServiceImpl mcqTestAssignmentService;

	@Scheduled(cron = "0 * * * * *")
	public void runSweep() {
		Date now = new Date();

		List<McqTestAssignment> expiredInProgress = mcqTestAssignmentRepo
			.findAllByStatusAndDeadlineAtBeforeAndDeleted("IN_PROGRESS", now, false);
		for (McqTestAssignment assignment : expiredInProgress) {
			try {
				mcqTestAssignmentService.autoSubmitExpired(assignment.getId());
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed: {}", assignment.getId(), ex.getMessage(), ex);
			}
		}

		// Scheduled exams whose "not after" start window closed while the candidate never started.
		List<McqTestAssignment> missedWindow = mcqTestAssignmentRepo
			.findAllByStatusAndScheduledEndAtBeforeAndDeleted("ASSIGNED", now, false);
		for (McqTestAssignment assignment : missedWindow) {
			try {
				mcqTestAssignmentService.expireUnstarted(assignment.getId());
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed to expire: {}", assignment.getId(), ex.getMessage(), ex);
			}
		}

		// Per-question timers that ran out without the client-side auto-advance firing.
		List<McqTestAssignment> expiredQuestions = mcqTestAssignmentRepo
			.findAllByStatusAndCurrentQuestionDeadlineAtBeforeAndDeleted("IN_PROGRESS", now, false);
		for (McqTestAssignment assignment : expiredQuestions) {
			try {
				mcqTestAssignmentService.autoAdvanceOrSubmitIfQuestionExpired(assignment.getId());
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed to auto-advance: {}", assignment.getId(), ex.getMessage(), ex);
			}
		}
	}
}
