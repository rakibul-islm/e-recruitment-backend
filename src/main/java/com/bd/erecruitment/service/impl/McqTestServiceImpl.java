package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.McqTestReqDto;
import com.bd.erecruitment.dto.res.McqQuestionResDTO;
import com.bd.erecruitment.dto.res.McqTestResDTO;
import com.bd.erecruitment.entity.McqQuestion;
import com.bd.erecruitment.entity.McqTest;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.McqQuestionRepo;
import com.bd.erecruitment.repository.McqTestRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// Same per-company scoping rule as McqQuestionServiceImpl/JobCircularServiceImpl.
@Service
public class McqTestServiceImpl extends AbstractBaseService<McqTest> implements BaseService<McqTestResDTO, McqTestReqDto> {

	private final McqQuestionRepo mcqQuestionRepo;

	public McqTestServiceImpl(McqTestRepo mcqTestRepo, McqQuestionRepo mcqQuestionRepo) {
		super(mcqTestRepo);
		this.mcqQuestionRepo = mcqQuestionRepo;
	}

	@Override
	public Response<McqTestResDTO> find(Long id) {
		McqTest test = findByIdOrThrow(id, "Test not found");
		if (isScopedRecruiter() && !companyMatchesCaller(test.getCompanyId())) returnNotFoundException("Test not found");
		return getSuccessResponse("Test found", new McqTestResDTO(test));
	}

	@Transactional
	@Override
	public Response<McqTestResDTO> save(McqTestReqDto reqDto) {
		validateForm(reqDto);
		McqTest bean = reqDto.getBean();
		if (StringUtils.isBlank(bean.getStatus())) bean.setStatus("DRAFT");
		if (isScopedRecruiter()) bean.setCompanyId(getLoggedInUserDetails().getCompanyId());
		McqTest test = createEntity(bean);
		return getCreatedResponse("Test saved successfully", new McqTestResDTO(test));
	}

	@Transactional
	@Override
	public Response<McqTestResDTO> update(McqTestReqDto reqDto) {
		McqTest existing = findByIdOrThrow(reqDto.getId(), "Test not found");
		if (isScopedRecruiter() && !companyMatchesCaller(existing.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's tests");
		validateForm(reqDto);

		existing.setName(reqDto.getName())
			.setDescription(reqDto.getDescription())
			.setDurationMinutes(reqDto.getDurationMinutes())
			.setPassingScorePercent(reqDto.getPassingScorePercent())
			.setQuestionSelectionCount(reqDto.getQuestionSelectionCount())
			.setSecondsPerQuestion(reqDto.getSecondsPerQuestion())
			.setShuffleQuestions(reqDto.isShuffleQuestions())
			.setShuffleOptions(reqDto.isShuffleOptions());
		if (StringUtils.isNotBlank(reqDto.getStatus())) existing.setStatus(reqDto.getStatus());
		existing.getQuestionIds().clear();
		existing.getQuestionIds().addAll(reqDto.getQuestionIds());

		existing = updateEntity(existing);
		return getSuccessResponse("Test updated successfully", new McqTestResDTO(existing));
	}

	@Transactional
	@Override
	public Response<McqTestResDTO> delete(Long id) {
		McqTest test = findByIdOrThrow(id, "Test not found");
		if (isScopedRecruiter() && !companyMatchesCaller(test.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's tests");
		deleteEntity(test);
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<McqTestResDTO> remove(Long id) {
		McqTest test = findByIdOrThrow(id, "Test not found");
		if (isScopedRecruiter() && !companyMatchesCaller(test.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's tests");
		removeEntity(test);
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<McqTestResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		if (isScopedRecruiter()) {
			MyUserDetail me = getLoggedInUserDetails();
			filters = new HashMap<>(filters);
			filters.put("companyId", me.getCompanyId() == null ? "-1" : String.valueOf(me.getCompanyId()));
		}
		return genericFilter(filters, pageable, isPageable, McqTestResDTO.class);
	}

	private boolean companyMatchesCaller(Long companyId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getCompanyId() != null && me.getCompanyId().equals(companyId);
	}

	private void validateForm(McqTestReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Test name is required");
		if (Objects.isNull(reqDto.getDurationMinutes()) || reqDto.getDurationMinutes() < 1) returnErrorException("Duration is required");
		if (Objects.isNull(reqDto.getPassingScorePercent()) || reqDto.getPassingScorePercent() < 0 || reqDto.getPassingScorePercent() > 100)
			returnErrorException("Passing score must be between 0 and 100");
		if (reqDto.getQuestionIds() == null || reqDto.getQuestionIds().isEmpty()) returnErrorException("At least one question is required");
		if (reqDto.getQuestionSelectionCount() != null && reqDto.getQuestionSelectionCount() > reqDto.getQuestionIds().size())
			returnErrorException("Question selection count can't exceed the pool size");
		if (reqDto.getSecondsPerQuestion() != null && reqDto.getSecondsPerQuestion() < 5)
			returnErrorException("Time per question must be at least 5 seconds");

		List<McqQuestion> questions = mcqQuestionRepo.findAllByIdInAndDeleted(reqDto.getQuestionIds(), false);
		if (questions.size() != reqDto.getQuestionIds().size()) returnErrorException("One or more selected questions could not be found");
		if (questions.stream().anyMatch(q -> !"APPROVED".equals(q.getStatus())))
			returnErrorException("Only approved questions may be added to a test");
	}
}
