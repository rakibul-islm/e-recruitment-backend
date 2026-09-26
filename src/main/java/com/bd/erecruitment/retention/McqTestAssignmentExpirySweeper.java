package com.bd.erecruitment.retention;

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

	@Scheduled(cron = "0 * * * * *", zone = "Asia/Dhaka")
	public void runSweep() {
		Date now = new Date();

		List<Long> expiredInProgress = mcqTestAssignmentRepo
			.findIdsByStatusAndDeadlineAtBeforeAndDeleted("IN_PROGRESS", now, false);
		for (Long id : expiredInProgress) {
			try {
				mcqTestAssignmentService.autoSubmitExpired(id);
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed: {}", id, ex.getMessage(), ex);
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "McqTestAssignmentExpirySweeper.autoSubmitExpired:" + id);
			}
		}

		List<Long> missedWindow = mcqTestAssignmentRepo
			.findIdsByStatusAndScheduledEndAtBeforeAndDeleted("ASSIGNED", now, false);
		for (Long id : missedWindow) {
			try {
				mcqTestAssignmentService.expireUnstarted(id);
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed to expire: {}", id, ex.getMessage(), ex);
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "McqTestAssignmentExpirySweeper.expireUnstarted:" + id);
			}
		}

		List<Long> expiredQuestions = mcqTestAssignmentRepo
			.findIdsByStatusAndCurrentQuestionDeadlineAtBeforeAndDeleted("IN_PROGRESS", now, false);
		for (Long id : expiredQuestions) {
			try {
				mcqTestAssignmentService.autoAdvanceOrSubmitIfQuestionExpired(id);
			} catch (Exception ex) {
				log.error("[McqTestAssignmentExpirySweeper] assignment {}: failed to auto-advance: {}", id, ex.getMessage(), ex);
				exceptionLogWriter.log(ex, 0, ex.getMessage(), "McqTestAssignmentExpirySweeper.autoAdvanceOrSubmit:" + id);
			}
		}
	}
}
