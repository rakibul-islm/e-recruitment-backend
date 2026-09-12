package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.AssignMcqTestReqDto;
import com.bd.erecruitment.dto.req.BulkAssignMcqTestReqDto;
import com.bd.erecruitment.dto.req.SubmitAnswerReqDto;
import com.bd.erecruitment.dto.res.McqAssignmentQuestionReviewDto;
import com.bd.erecruitment.dto.res.McqTestAssignmentResDTO;
import com.bd.erecruitment.dto.res.McqTestAttemptQuestionDto;
import com.bd.erecruitment.entity.*;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.ApplicationStatusHistoryRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.McqQuestionRepo;
import com.bd.erecruitment.repository.McqTestAssignmentQuestionRepo;
import com.bd.erecruitment.repository.McqTestAssignmentRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Assign/start/answer/submit for one candidate's attempt at an McqTest, plus the recruiter's
// assign/review actions. Staff-vs-candidate access is manual (same STAFF_AUTHORITY proxy pattern
// as InterviewServiceImpl/ApplicationServiceImpl) - start/answer/submit are candidate-own ONLY
// (no staff bypass, unlike read access), since a recruiter must never be able to drive a
// candidate's own timed attempt.
@Slf4j
@Service
public class McqTestAssignmentServiceImpl extends AbstractBaseService<McqTestAssignment> {

	private static final String STAFF_AUTHORITY = "job-circular:write";

	private final McqTestAssignmentRepo mcqTestAssignmentRepo;
	private final McqTestAssignmentQuestionRepo mcqTestAssignmentQuestionRepo;
	private final McqTestRepo mcqTestRepo;
	private final McqQuestionRepo mcqQuestionRepo;
	private final ApplicationRepo applicationRepo;
	private final ApplicationStatusHistoryRepo historyRepo;
	private final JobCircularRepo jobCircularRepo;
	private final UserRepo userRepo;
	private final MailService mailService;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	public McqTestAssignmentServiceImpl(McqTestAssignmentRepo mcqTestAssignmentRepo,
			McqTestAssignmentQuestionRepo mcqTestAssignmentQuestionRepo, McqTestRepo mcqTestRepo,
			McqQuestionRepo mcqQuestionRepo, ApplicationRepo applicationRepo, ApplicationStatusHistoryRepo historyRepo,
			JobCircularRepo jobCircularRepo, UserRepo userRepo, MailService mailService) {
		super(mcqTestAssignmentRepo);
		this.mcqTestAssignmentRepo = mcqTestAssignmentRepo;
		this.mcqTestAssignmentQuestionRepo = mcqTestAssignmentQuestionRepo;
		this.mcqTestRepo = mcqTestRepo;
		this.mcqQuestionRepo = mcqQuestionRepo;
		this.applicationRepo = applicationRepo;
		this.historyRepo = historyRepo;
		this.jobCircularRepo = jobCircularRepo;
		this.userRepo = userRepo;
		this.mailService = mailService;
	}

	@Transactional
	public Response<McqTestAssignmentResDTO> assign(AssignMcqTestReqDto reqDto) {
		requireStaff("assign tests");
		if (reqDto.getApplicationId() == null) returnErrorException("Application is required");
		if (reqDto.getMcqTestId() == null) returnErrorException("Test is required");
		validateSchedule(reqDto.getScheduledAt(), reqDto.getScheduledEndAt());

		Application application = applicationRepo.findByIdAndDeleted(reqDto.getApplicationId(), false)
			.orElseThrow(() -> new NotFoundException("Application not found"));
		McqTest test = requireAssignableTest(reqDto.getMcqTestId());

		McqTestAssignment assignment = doAssign(application, test, reqDto.getScheduledAt(), reqDto.getScheduledEndAt(),
			getLoggedInUserDetails().getUsername());
		return getCreatedResponse("Test assigned successfully", toDto(assignment, test));
	}

	@Transactional
	public Response<McqTestAssignmentResDTO> bulkAssign(BulkAssignMcqTestReqDto reqDto) {
		requireStaff("assign tests");
		if (reqDto.getApplicationIds() == null || reqDto.getApplicationIds().isEmpty())
			returnErrorException("At least one applicant is required");
		if (reqDto.getMcqTestId() == null) returnErrorException("Test is required");
		validateSchedule(reqDto.getScheduledAt(), reqDto.getScheduledEndAt());

		McqTest test = requireAssignableTest(reqDto.getMcqTestId());
		String actor = getLoggedInUserDetails().getUsername();

		List<McqTestAssignmentResDTO> created = new ArrayList<>();
		int skipped = 0;
		for (Long applicationId : reqDto.getApplicationIds()) {
			try {
				Application application = applicationRepo.findByIdAndDeleted(applicationId, false)
					.orElseThrow(() -> new NotFoundException("Application not found"));
				McqTestAssignment assignment = doAssign(application, test, reqDto.getScheduledAt(), reqDto.getScheduledEndAt(), actor);
				created.add(toDto(assignment, test));
			} catch (Exception ex) {
				skipped++;
				log.warn("[McqTestAssignmentServiceImpl] bulk assign skipped application {}: {}", applicationId, ex.getMessage());
			}
		}

		String message = "Assigned to " + created.size() + " of " + reqDto.getApplicationIds().size() + " selected applicant(s)"
			+ (skipped > 0 ? " (" + skipped + " skipped)" : "");
		Response<McqTestAssignmentResDTO> response = new Response<>();
		response.setCode(201);
		response.setSuccess(true);
		response.setMessage(message);
		response.setList(created);
		return response;
	}

	// Shared by assign() and bulkAssign() - freezes/randomizes the question set, persists the
	// assignment + its frozen question rows, bumps the application into screening, and notifies.
	private McqTestAssignment doAssign(Application application, McqTest test, Date scheduledAt, Date scheduledEndAt, String actor) {
		if (isScopedRecruiter() && !companyMatchesCaller(test.getCompanyId()))
			throw new ForbiddenException("You may only assign your own company's tests");

		List<Long> selected = selectQuestionIds(test);
		if (selected.isEmpty()) returnErrorException("This test has no questions to assign");
		Map<Long, McqQuestion> questionById = mcqQuestionRepo.findAllByIdInAndDeleted(selected, false).stream()
			.collect(Collectors.toMap(McqQuestion::getId, q -> q));

		Date now = new Date();

		McqTestAssignment assignment = new McqTestAssignment()
			.setApplicationId(application.getId())
			.setMcqTestId(test.getId())
			.setCandidateUserId(application.getCandidateUserId())
			.setStatus("ASSIGNED")
			.setAssignedOn(now)
			.setAssignedBy(actor)
			.setScheduledAt(scheduledAt)
			.setScheduledEndAt(scheduledEndAt)
			.setCurrentQuestionIndex(0)
			.setSecondsPerQuestionSnapshot(test.getSecondsPerQuestion())
			.setDurationMinutesSnapshot(test.getDurationMinutes())
			.setPassingScorePercentSnapshot(test.getPassingScorePercent());
		assignment = createEntity(assignment, actor);

		int order = 0;
		for (Long questionId : selected) {
			McqQuestion question = questionById.get(questionId);
			if (question == null) continue;
			McqTestAssignmentQuestion row = new McqTestAssignmentQuestion()
				.setAssignmentId(assignment.getId())
				.setQuestionId(question.getId())
				.setDisplayOrder(order++)
				.setQuestionTextSnapshot(question.getQuestionText());
			row.getOptions().addAll(freezeOptions(question, test.isShuffleOptions()));
			row.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
			mcqTestAssignmentQuestionRepo.save(row);
		}

		if ("APPLIED".equals(application.getStatus())) {
			application.setStatus("SCREENING").setStatusUpdatedOn(now).setStatusUpdatedBy(actor);
			applicationRepo.save(application);
		}
		recordHistory(application.getId(), application.getStatus(), "MCQ test assigned: " + test.getName(), actor);
		notifyAssigned(assignment, application, test);

		return assignment;
	}

	private McqTest requireAssignableTest(Long mcqTestId) {
		McqTest test = mcqTestRepo.findByIdAndDeleted(mcqTestId, false)
			.orElseThrow(() -> new NotFoundException("Test not found"));
		if (!"ACTIVE".equals(test.getStatus())) returnErrorException("Only active tests may be assigned");
		return test;
	}

	private void validateSchedule(Date scheduledAt, Date scheduledEndAt) {
		if (scheduledAt != null && scheduledEndAt != null && !scheduledEndAt.after(scheduledAt)) {
			returnErrorException("The exam window's close time must be after its open time");
		}
	}

	public Response<McqTestAssignmentResDTO> myAssignments() {
		MyUserDetail me = getLoggedInUserDetails();
		List<Application> myApplications = applicationRepo.findAllByCandidateUserIdAndDeletedOrderByAppliedOnDesc(me.getId(), false);
		List<McqTestAssignmentResDTO> list = myApplications.stream()
			.flatMap(app -> mcqTestAssignmentRepo.findAllByApplicationIdAndDeletedOrderByAssignedOnDesc(app.getId(), false).stream())
			.map(a -> toDto(a, null))
			.toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	public Response<McqTestAssignmentResDTO> findByApplication(Long applicationId) {
		getOwnedOrStaffApplication(applicationId);
		List<McqTestAssignmentResDTO> list = mcqTestAssignmentRepo
			.findAllByApplicationIdAndDeletedOrderByAssignedOnDesc(applicationId, false)
			.stream().map(a -> toDto(a, null)).toList();
		return getSuccessResponse(list.isEmpty() ? "No data found" : "Found", list);
	}

	public Response<McqTestAssignmentResDTO> find(Long id) {
		McqTestAssignment assignment = getOwnedOrStaffAssignment(id);
		return getSuccessResponse("Assignment found", toDto(assignment, null));
	}

	public Response<Object> getQuestions(Long id) {
		McqTestAssignment assignment = getOwnedOrStaffAssignment(id);
		MyUserDetail me = getLoggedInUserDetails();
		List<McqTestAssignmentQuestion> rows = mcqTestAssignmentQuestionRepo
			.findAllByAssignmentIdAndDeletedOrderByDisplayOrderAsc(assignment.getId(), false);

		List<?> mapped = isStaff(me)
			? rows.stream().map(this::toReviewDto).toList()
			: rows.stream().map(this::toAttemptDto).toList();
		List<Object> list = new ArrayList<>(mapped);

		Response<Object> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage(list.isEmpty() ? "No data found" : "Found");
		response.setList(list);
		return response;
	}

	@Transactional
	public Response<McqTestAssignmentResDTO> start(Long id) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		requireOwnAssignment(assignment);
		if ("SUBMITTED".equals(assignment.getStatus()) || "EXPIRED".equals(assignment.getStatus())) {
			returnErrorException("This test has already been completed");
		}
		Date now = new Date();
		if (assignment.getScheduledAt() != null && now.before(assignment.getScheduledAt())) {
			returnErrorException("This test opens at " + assignment.getScheduledAt());
		}
		// Defense-in-depth: the sweep normally flips a missed window to EXPIRED before this is ever
		// reached, but a candidate could race the sweep in the same minute the window closes.
		if (assignment.getScheduledEndAt() != null && now.after(assignment.getScheduledEndAt())) {
			returnErrorException("The window to start this test has closed");
		}
		if ("ASSIGNED".equals(assignment.getStatus())) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(now);
			cal.add(Calendar.MINUTE, assignment.getDurationMinutesSnapshot());
			assignment.setStatus("IN_PROGRESS").setStartedOn(now).setDeadlineAt(cal.getTime());
			if (assignment.getSecondsPerQuestionSnapshot() != null) {
				assignment.setCurrentQuestionDeadlineAt(questionDeadline(now, assignment.getSecondsPerQuestionSnapshot()));
			}
			assignment = updateEntity(assignment);
		}
		return getSuccessResponse("Test started", toDto(assignment, null));
	}

	private Date questionDeadline(Date from, int secondsPerQuestion) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(from);
		cal.add(Calendar.SECOND, secondsPerQuestion);
		return cal.getTime();
	}

	@Transactional
	public Response<McqTestAttemptQuestionDto> answer(Long id, SubmitAnswerReqDto reqDto) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		requireOwnAssignment(assignment);
		if (!"IN_PROGRESS".equals(assignment.getStatus())) returnErrorException("This test is not in progress");
		if (assignment.getDeadlineAt() != null && new Date().after(assignment.getDeadlineAt()))
			returnErrorException("Time is up for this test");

		McqTestAssignmentQuestion row = mcqTestAssignmentQuestionRepo
			.findByIdAndAssignmentIdAndDeleted(reqDto.getAssignmentQuestionId(), id, false)
			.orElseThrow(() -> new NotFoundException("Question not found"));
		// Server-enforced forward-only lock: a candidate (or a direct API call bypassing the UI)
		// can only ever write an answer to the current question, never one already advanced past.
		int currentIndex = assignment.getCurrentQuestionIndex() != null ? assignment.getCurrentQuestionIndex() : 0;
		if (row.getDisplayOrder() != currentIndex) {
			returnErrorException("You can only answer the current question");
		}
		row.setSelectedOptionKey(reqDto.getSelectedOptionKey()).setAnsweredOn(new Date());
		String actor = getLoggedInUserDetails().getUsername();
		row.setUpdatedBy(actor).setUpdatedOn(new Date()).setDeleted(false);
		row = mcqTestAssignmentQuestionRepo.save(row);

		return getSuccessResponse("Answer saved", toAttemptDto(row));
	}

	// Moves the candidate to the next question - the only way currentQuestionIndex changes, and it
	// only ever increases (clamped to the last question), never regresses. A page reload restores
	// currentIndex from this server value (see toDto()/start()), so an earlier question can never be
	// shown - let alone re-answered - again, closing the gap a purely client-side "currentIndex"
	// would leave open on reload.
	@Transactional
	public Response<McqTestAssignmentResDTO> advance(Long id) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		requireOwnAssignment(assignment);
		if (!"IN_PROGRESS".equals(assignment.getStatus())) returnErrorException("This test is not in progress");
		assignment = doAdvance(assignment, getLoggedInUserDetails().getUsername(), false);
		return getSuccessResponse("Advanced", toDto(assignment, null));
	}

	// Called by the sweeper for a candidate whose per-question timer expired without the
	// client-side auto-advance firing (lost connection, closed tab). If there's a next question,
	// advances to it (with a fresh per-question deadline); if this was the last question, submits
	// the whole attempt instead - same "system" actor / direct-repository-save pattern as
	// autoSubmitExpired/expireUnstarted.
	@Transactional
	public void autoAdvanceOrSubmitIfQuestionExpired(Long id) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		if (!"IN_PROGRESS".equals(assignment.getStatus())) return;
		if (assignment.getCurrentQuestionIndex() != null && assignment.getCurrentQuestionIndex() < lastQuestionIndex(assignment)) {
			doAdvance(assignment, "system", true);
		} else {
			doSubmit(assignment, "AUTO", "system", true);
		}
	}

	private McqTestAssignment doAdvance(McqTestAssignment assignment, String actor, boolean fromSystem) {
		int currentIndex = assignment.getCurrentQuestionIndex() != null ? assignment.getCurrentQuestionIndex() : 0;
		int lastIndex = lastQuestionIndex(assignment);
		if (currentIndex < lastIndex) {
			assignment.setCurrentQuestionIndex(currentIndex + 1);
			if (assignment.getSecondsPerQuestionSnapshot() != null) {
				assignment.setCurrentQuestionDeadlineAt(questionDeadline(new Date(), assignment.getSecondsPerQuestionSnapshot()));
			}
			if (fromSystem) {
				assignment.setUpdatedBy(actor).setUpdatedOn(new Date()).setDeleted(false);
				assignment = mcqTestAssignmentRepo.save(assignment);
			} else {
				assignment = updateEntity(assignment);
			}
		}
		return assignment;
	}

	private int lastQuestionIndex(McqTestAssignment assignment) {
		return (int) mcqTestAssignmentQuestionRepo
			.findAllByAssignmentIdAndDeletedOrderByDisplayOrderAsc(assignment.getId(), false).stream().count() - 1;
	}

	@Transactional
	public Response<McqTestAssignmentResDTO> submit(Long id) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		requireOwnAssignment(assignment);
		if ("SUBMITTED".equals(assignment.getStatus()) || "EXPIRED".equals(assignment.getStatus())) {
			return getSuccessResponse("Already submitted", toDto(assignment, null));
		}
		if (!"IN_PROGRESS".equals(assignment.getStatus())) returnErrorException("This test has not been started");

		String actor = getLoggedInUserDetails().getUsername();
		McqTestAssignment saved = doSubmit(assignment, "MANUAL", actor, false);
		return getSuccessResponse("Test submitted", toDto(saved, null));
	}

	// Called by McqTestAssignmentExpirySweeper for a candidate who let the timer run out without
	// the client-side auto-submit firing (lost connection, closed tab). No security context is
	// active on a scheduled job, so grading is stamped as "system" and persisted directly via the
	// repository rather than through updateEntity (which needs a logged-in user).
	@Transactional
	public void autoSubmitExpired(Long id) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		if (!"IN_PROGRESS".equals(assignment.getStatus())) return;
		doSubmit(assignment, "AUTO", "system", true);
	}

	// Called by McqTestAssignmentExpirySweeper for a scheduled exam whose "not after" start window
	// closed while the candidate never opened it at all (still ASSIGNED). No grading involved -
	// this is a pure status flip, same "system" actor / direct-repository-save pattern as
	// autoSubmitExpired since no security context is active on a scheduled job.
	@Transactional
	public void expireUnstarted(Long id) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		if (!"ASSIGNED".equals(assignment.getStatus())) return;

		Date now = new Date();
		assignment.setStatus("EXPIRED").setUpdatedBy("system").setUpdatedOn(now).setDeleted(false);
		McqTestAssignment saved = mcqTestAssignmentRepo.save(assignment);

		Application application = applicationRepo.findByIdAndDeleted(saved.getApplicationId(), false).orElse(null);
		if (application != null) {
			recordHistory(application.getId(), application.getStatus(), "MCQ test window closed unstarted", "system");
		}
	}

	private McqTestAssignment doSubmit(McqTestAssignment assignment, String via, String actor, boolean fromSystem) {
		List<McqTestAssignmentQuestion> rows = mcqTestAssignmentQuestionRepo
			.findAllByAssignmentIdAndDeletedOrderByDisplayOrderAsc(assignment.getId(), false);
		Date now = new Date();
		int correctCount = 0;
		for (McqTestAssignmentQuestion row : rows) {
			boolean isCorrect = row.getSelectedOptionKey() != null && row.getOptions().stream()
				.anyMatch(o -> o.getOptionKey().equals(row.getSelectedOptionKey()) && o.isCorrect());
			row.setCorrect(isCorrect);
			if (isCorrect) correctCount++;
			row.setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
			mcqTestAssignmentQuestionRepo.save(row);
		}
		int totalCount = rows.size();
		int scorePercent = totalCount == 0 ? 0 : Math.round(100f * correctCount / totalCount);
		boolean passed = assignment.getPassingScorePercentSnapshot() != null
			&& scorePercent >= assignment.getPassingScorePercentSnapshot();

		assignment.setStatus("SUBMITTED")
			.setSubmittedOn(now)
			.setSubmittedVia(via)
			.setCorrectCount(correctCount)
			.setTotalCount(totalCount)
			.setScorePercent(scorePercent)
			.setPassed(passed);

		McqTestAssignment saved;
		if (fromSystem) {
			assignment.setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
			saved = mcqTestAssignmentRepo.save(assignment);
		} else {
			saved = updateEntity(assignment);
		}

		recordAndNotifySubmission(saved, actor);
		return saved;
	}

	private void recordAndNotifySubmission(McqTestAssignment assignment, String actor) {
		Application application = applicationRepo.findByIdAndDeleted(assignment.getApplicationId(), false).orElse(null);
		if (application == null) return;

		boolean passed = Boolean.TRUE.equals(assignment.getPassed());
		recordHistory(application.getId(), application.getStatus(),
			"MCQ test completed: " + (passed ? "PASSED" : "FAILED") + " (" + assignment.getScorePercent() + "%)", actor);

		try {
			User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			McqTest test = mcqTestRepo.findByIdAndDeleted(assignment.getMcqTestId(), false).orElse(null);
			if (candidate == null || job == null || test == null) return;
			mailService.sendMcqTestResultEmail(candidate.getEmail(), candidate.getFullName(), job.getJobTitle(),
				test.getName(), assignment.getScorePercent(), passed, frontendBaseUrl + "/my/applications/" + application.getId());
		} catch (Exception e) {
			log.warn("Failed to send MCQ result email for assignment {}: {}", assignment.getId(), e.getMessage());
		}
	}

	private void notifyAssigned(McqTestAssignment assignment, Application application, McqTest test) {
		try {
			User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			if (candidate == null || job == null) return;
			mailService.sendMcqTestAssignedEmail(candidate.getEmail(), candidate.getFullName(), job.getJobTitle(),
				test.getName(), test.getDurationMinutes(), frontendBaseUrl + "/my/applications/" + application.getId());
		} catch (Exception e) {
			log.warn("Failed to send MCQ test-assigned email for assignment {}: {}", assignment.getId(), e.getMessage());
		}
	}

	// Random subset (when questionSelectionCount is set) is always picked via shuffle, independent
	// of shuffleQuestions - that flag controls display ORDER only. If a subset was picked but
	// shuffleQuestions is off, the subset is restored to original pool order for display.
	private List<Long> selectQuestionIds(McqTest test) {
		List<Long> pool = new ArrayList<>(test.getQuestionIds());
		List<Long> selected;
		Integer count = test.getQuestionSelectionCount();
		if (count != null && count < pool.size()) {
			List<Long> shuffledForPick = new ArrayList<>(pool);
			Collections.shuffle(shuffledForPick);
			selected = new ArrayList<>(shuffledForPick.subList(0, count));
			if (!test.isShuffleQuestions()) {
				selected.sort(Comparator.comparingInt(pool::indexOf));
			}
		} else {
			selected = new ArrayList<>(pool);
		}
		if (test.isShuffleQuestions()) {
			Collections.shuffle(selected);
		}
		return selected;
	}

	// Options are copied in original order, then handed a (possibly shuffled) displayOrder - the
	// optionKey/correct identity never moves, only where it renders.
	private List<McqAssignmentOptionItem> freezeOptions(McqQuestion question, boolean shuffleOptions) {
		List<McqOptionItem> source = question.getOptions().stream()
			.sorted(Comparator.comparingInt(McqOptionItem::getDisplayOrder))
			.toList();
		List<Integer> order = new ArrayList<>();
		for (int i = 0; i < source.size(); i++) order.add(i);
		if (shuffleOptions) Collections.shuffle(order);

		List<McqAssignmentOptionItem> result = new ArrayList<>();
		for (int i = 0; i < source.size(); i++) {
			McqOptionItem src = source.get(i);
			result.add(new McqAssignmentOptionItem()
				.setOptionKey(src.getOptionKey())
				.setOptionText(src.getOptionText())
				.setCorrect(src.isCorrect())
				.setDisplayOrder(order.get(i)));
		}
		return result;
	}

	private McqTestAttemptQuestionDto toAttemptDto(McqTestAssignmentQuestion q) {
		McqTestAttemptQuestionDto dto = new McqTestAttemptQuestionDto();
		dto.setId(q.getId());
		dto.setDisplayOrder(q.getDisplayOrder());
		dto.setQuestionText(q.getQuestionTextSnapshot());
		dto.setSelectedOptionKey(q.getSelectedOptionKey());
		q.getOptions().stream().sorted(Comparator.comparingInt(McqAssignmentOptionItem::getDisplayOrder)).forEach(o -> {
			McqTestAttemptQuestionDto.Option opt = new McqTestAttemptQuestionDto.Option();
			opt.setOptionKey(o.getOptionKey());
			opt.setOptionText(o.getOptionText());
			opt.setDisplayOrder(o.getDisplayOrder());
			dto.getOptions().add(opt);
		});
		return dto;
	}

	private McqAssignmentQuestionReviewDto toReviewDto(McqTestAssignmentQuestion q) {
		McqAssignmentQuestionReviewDto dto = new McqAssignmentQuestionReviewDto();
		dto.setId(q.getId());
		dto.setDisplayOrder(q.getDisplayOrder());
		dto.setQuestionText(q.getQuestionTextSnapshot());
		dto.setSelectedOptionKey(q.getSelectedOptionKey());
		dto.setCorrect(q.getCorrect());
		q.getOptions().stream().sorted(Comparator.comparingInt(McqAssignmentOptionItem::getDisplayOrder)).forEach(o -> {
			McqAssignmentQuestionReviewDto.Option opt = new McqAssignmentQuestionReviewDto.Option();
			opt.setOptionKey(o.getOptionKey());
			opt.setOptionText(o.getOptionText());
			opt.setCorrect(o.isCorrect());
			opt.setDisplayOrder(o.getDisplayOrder());
			dto.getOptions().add(opt);
		});
		return dto;
	}

	private McqTestAssignmentResDTO toDto(McqTestAssignment assignment, McqTest testHint) {
		McqTestAssignmentResDTO dto = new McqTestAssignmentResDTO(assignment);
		McqTest test = testHint != null ? testHint : mcqTestRepo.findByIdAndDeleted(assignment.getMcqTestId(), false).orElse(null);
		if (test != null) dto.setTestName(test.getName());

		Application application = applicationRepo.findByIdAndDeleted(assignment.getApplicationId(), false).orElse(null);
		if (application != null) {
			JobCircular job = jobCircularRepo.findByIdAndDeleted(application.getJobCircularId(), false).orElse(null);
			if (job != null) dto.setJobTitle(job.getJobTitle());
			User candidate = userRepo.findByIdAndDeleted(application.getCandidateUserId(), false).orElse(null);
			if (candidate != null) dto.setCandidateName(candidate.getFullName());
		}
		return dto;
	}

	private void recordHistory(Long applicationId, String status, String note, String actor) {
		Date now = new Date();
		ApplicationStatusHistory history = new ApplicationStatusHistory()
			.setApplicationId(applicationId)
			.setStatus(status)
			.setNote(note)
			.setChangedBy(actor)
			.setChangedOn(now);
		history.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		historyRepo.save(history);
	}

	private Application getOwnedOrStaffApplication(Long applicationId) {
		Application application = applicationRepo.findByIdAndDeleted(applicationId, false)
			.orElseThrow(() -> new NotFoundException("Application not found"));
		MyUserDetail me = getLoggedInUserDetails();
		if (!application.getCandidateUserId().equals(me.getId()) && !isStaff(me)) {
			throw new ForbiddenException("Access denied");
		}
		return application;
	}

	private McqTestAssignment getOwnedOrStaffAssignment(Long id) {
		McqTestAssignment assignment = findByIdOrThrow(id, "Assignment not found");
		MyUserDetail me = getLoggedInUserDetails();
		if (!assignment.getCandidateUserId().equals(me.getId()) && !isStaff(me)) {
			throw new ForbiddenException("Access denied");
		}
		return assignment;
	}

	private void requireOwnAssignment(McqTestAssignment assignment) {
		MyUserDetail me = getLoggedInUserDetails();
		if (me == null || !assignment.getCandidateUserId().equals(me.getId())) {
			throw new ForbiddenException("Access denied");
		}
	}

	private void requireStaff(String action) {
		if (!isStaff(getLoggedInUserDetails())) {
			throw new ForbiddenException("Only recruiters/admins may " + action);
		}
	}

	private boolean isStaff(MyUserDetail user) {
		return user != null && user.getAuthorities().stream().anyMatch(a ->
			STAFF_AUTHORITY.equals(a.getAuthority()) || "SUPER_ADMIN".equals(a.getAuthority()));
	}

	private boolean companyMatchesCaller(Long companyId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getCompanyId() != null && me.getCompanyId().equals(companyId);
	}
}
