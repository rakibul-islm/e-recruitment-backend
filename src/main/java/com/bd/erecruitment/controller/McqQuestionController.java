package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.McqGenerateQuestionsReqDto;
import com.bd.erecruitment.dto.req.McqQuestionReqDto;
import com.bd.erecruitment.dto.res.McqQuestionResDTO;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.impl.McqQuestionGenerationServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/mcq-question")
@Tag(name = "5.1 MCQ Question Bank", description = "Reusable multiple-choice question bank")
public class McqQuestionController extends AbstractBaseController<McqQuestionResDTO, McqQuestionReqDto> {

	private final McqQuestionGenerationServiceImpl generationService;

	public McqQuestionController(BaseService<McqQuestionResDTO, McqQuestionReqDto> service, McqQuestionGenerationServiceImpl generationService) {
		super(service);
		this.generationService = generationService;
	}

	@Operation(summary = "Generate draft MCQ questions from a skill/topic prompt (recruiter/admin)")
	@PostMapping("/generate")
	public Response<McqQuestionResDTO> generate(@RequestBody McqGenerateQuestionsReqDto req) {
		return generationService.generate(req);
	}
}
