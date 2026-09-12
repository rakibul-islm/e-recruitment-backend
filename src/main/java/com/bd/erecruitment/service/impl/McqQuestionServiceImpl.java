package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.McqOptionReqDto;
import com.bd.erecruitment.dto.req.McqQuestionReqDto;
import com.bd.erecruitment.dto.res.McqQuestionResDTO;
import com.bd.erecruitment.entity.McqOptionItem;
import com.bd.erecruitment.entity.McqQuestion;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.McqQuestionRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// A plain recruiter (isScopedRecruiter()) may only see/manage their own company's bank questions -
// same rule and reasoning as JobCircularServiceImpl. Candidates never reach this service at all
// (no mcq-question:* authority is granted to REGISTERED_USER - see RoleData).
@Service
public class McqQuestionServiceImpl extends AbstractBaseService<McqQuestion> implements BaseService<McqQuestionResDTO, McqQuestionReqDto> {

	public McqQuestionServiceImpl(McqQuestionRepo mcqQuestionRepo) {
		super(mcqQuestionRepo);
	}

	@Override
	public Response<McqQuestionResDTO> find(Long id) {
		McqQuestion question = findByIdOrThrow(id, "Question not found");
		if (isScopedRecruiter() && !companyMatchesCaller(question.getCompanyId())) returnNotFoundException("Question not found");
		return getSuccessResponse("Question found", new McqQuestionResDTO(question));
	}

	@Transactional
	@Override
	public Response<McqQuestionResDTO> save(McqQuestionReqDto reqDto) {
		validateForm(reqDto);
		McqQuestion bean = reqDto.getBean();
		if (StringUtils.isBlank(bean.getStatus())) bean.setStatus("APPROVED");
		bean.setSource("MANUAL");
		if (isScopedRecruiter()) bean.setCompanyId(getLoggedInUserDetails().getCompanyId());
		McqQuestion question = createEntity(bean);
		return getCreatedResponse("Question saved successfully", new McqQuestionResDTO(question));
	}

	@Transactional
	@Override
	public Response<McqQuestionResDTO> update(McqQuestionReqDto reqDto) {
		McqQuestion existing = findByIdOrThrow(reqDto.getId(), "Question not found");
		if (isScopedRecruiter() && !companyMatchesCaller(existing.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's questions");
		validateForm(reqDto);

		existing.setQuestionText(reqDto.getQuestionText())
			.setSkillTag(reqDto.getSkillTag())
			.setDifficulty(reqDto.getDifficulty())
			.setExplanation(reqDto.getExplanation());
		if (StringUtils.isNotBlank(reqDto.getStatus())) existing.setStatus(reqDto.getStatus());

		existing.getOptions().clear();
		int order = 0;
		for (McqOptionReqDto opt : reqDto.getOptions()) {
			existing.getOptions().add(new McqOptionItem()
				.setOptionKey(String.valueOf((char) ('A' + order)))
				.setOptionText(opt.getOptionText())
				.setCorrect(opt.isCorrect())
				.setDisplayOrder(order++));
		}

		existing = updateEntity(existing);
		return getSuccessResponse("Question updated successfully", new McqQuestionResDTO(existing));
	}

	@Transactional
	@Override
	public Response<McqQuestionResDTO> delete(Long id) {
		McqQuestion question = findByIdOrThrow(id, "Question not found");
		if (isScopedRecruiter() && !companyMatchesCaller(question.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's questions");
		deleteEntity(question);
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<McqQuestionResDTO> remove(Long id) {
		McqQuestion question = findByIdOrThrow(id, "Question not found");
		if (isScopedRecruiter() && !companyMatchesCaller(question.getCompanyId()))
			throw new ForbiddenException("You may only manage your own company's questions");
		removeEntity(question);
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<McqQuestionResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		if (isScopedRecruiter()) {
			MyUserDetail me = getLoggedInUserDetails();
			filters = new HashMap<>(filters);
			filters.put("companyId", me.getCompanyId() == null ? "-1" : String.valueOf(me.getCompanyId()));
		}
		return genericFilter(filters, pageable, isPageable, McqQuestionResDTO.class);
	}

	private boolean companyMatchesCaller(Long companyId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getCompanyId() != null && me.getCompanyId().equals(companyId);
	}

	private void validateForm(McqQuestionReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getQuestionText())) returnErrorException("Question text is required");
		if (StringUtils.isBlank(reqDto.getDifficulty())) returnErrorException("Difficulty is required");
		if (reqDto.getOptions() == null || reqDto.getOptions().size() < 2) returnErrorException("At least two options are required");
		if (reqDto.getOptions().stream().noneMatch(McqOptionReqDto::isCorrect)) returnErrorException("Exactly one option must be marked correct");
		if (reqDto.getOptions().stream().filter(McqOptionReqDto::isCorrect).count() > 1) returnErrorException("Only one option may be marked correct");
	}
}
