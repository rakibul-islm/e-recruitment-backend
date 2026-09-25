package com.bd.erecruitment.retention;

import com.bd.erecruitment.entity.McqTestAssignment;
import com.bd.erecruitment.exception.ExceptionLogWriter;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.service.impl.McqTestAssignmentServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class McqTestAssignmentExpirySweeper {

	private final McqTestAssignmentRepo mcqTestAssignmentRepo;
	private final McqTestAssignmentServiceImpl mcqTestAssignmentService;
	private final ExceptionLogWriter exceptionLogWriter;

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
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "McqTestAssignmentExpirySweeper.autoSubmitExpired:" + assignment.getId());
			}
		}

		List<McqTestAssignment> missedWindow = mcqTestAssignmentRepo
			.findAllByStatusAndScheduledEndAtBeforeAndDeleted("ASSIGNED", now, false);
		for (McqTestAssignment assignment : missedWindow) {
			try {
				mcqTestAssignmentService.expireUnstarted(assignment.getId());
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed to expire: {}", assignment.getId(), ex.getMessage(), ex);
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "McqTestAssignmentExpirySweeper.expireUnstarted:" + assignment.getId());
			}
		}

		List<McqTestAssignment> expiredQuestions = mcqTestAssignmentRepo
			.findAllByStatusAndCurrentQuestionDeadlineAtBeforeAndDeleted("IN_PROGRESS", now, false);
		for (McqTestAssignment assignment : expiredQuestions) {
			try {
				mcqTestAssignmentService.autoAdvanceOrSubmitIfQuestionExpired(assignment.getId());
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed to auto-advance: {}", assignment.getId(), ex.getMessage(), ex);
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "McqTestAssignmentExpirySweeper.autoAdvanceOrSubmit:" + assignment.getId());
			}
		}
	}
}
