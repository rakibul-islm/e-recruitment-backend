package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.McqGenerateQuestionsReqDto;
import com.bd.erecruitment.dto.res.McqQuestionResDTO;
import com.bd.erecruitment.entity.McqOptionItem;
import com.bd.erecruitment.entity.McqQuestion;
import com.bd.erecruitment.exception.ApiException;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.McqQuestionRepo;
import com.bd.erecruitment.util.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Reuses the same free-tier Google Gemini integration as JobPostingAiServiceImpl (same api-key/
// model config, same retry/parsing shape) rather than adding a new paid AI dependency. Generated
// questions land as DRAFT/AI_GENERATED, requiring explicit recruiter review/approval before
// they're usable in a test - never auto-published. Extends AbstractBaseService<McqQuestion>
// purely to reuse its createEntity() (audit-stamped fields + free audit-diff logging), not
// because this service owns full CRUD for McqQuestion (McqQuestionServiceImpl does that).
@Slf4j
@Service
public class McqQuestionGenerationServiceImpl extends AbstractBaseService<McqQuestion> {

	private static final String STAFF_AUTHORITY = "job-circular:write";
	private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
	private static final int MAX_ATTEMPTS = 3;
	private static final long RETRY_BACKOFF_MILLIS = 1000;
	private static final int DEFAULT_COUNT = 5;
	private static final int MAX_COUNT = 20;

	private final RestTemplate restTemplate = buildRestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.ai.gemini.api-key:}")
	private String apiKey;

	@Value("${app.ai.gemini.model:gemini-3.6-flash}")
	private String model;

	public McqQuestionGenerationServiceImpl(McqQuestionRepo mcqQuestionRepo) {
		super(mcqQuestionRepo);
	}

	public Response<McqQuestionResDTO> generate(McqGenerateQuestionsReqDto req) {
		requireStaff();

		if (StringUtils.isBlank(req.getSkillTag())) {
			throw new BadRequestException("A topic/skill is required to generate questions");
		}
		int count = req.getCount() == null ? DEFAULT_COUNT : Math.min(Math.max(req.getCount(), 1), MAX_COUNT);
		if (StringUtils.isBlank(apiKey)) {
			throw new ApiException(503, "AI question generation is not configured on this server");
		}

		String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildGeminiRequest(req, count), headers);

		String rawResponse = null;
		RestClientException lastFailure = null;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				rawResponse = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
				lastFailure = null;
				break;
			} catch (RestClientException e) {
				lastFailure = e;
				log.warn("Gemini MCQ generation call failed (attempt {}/{}): {}", attempt, MAX_ATTEMPTS, e.getMessage());
				if (attempt < MAX_ATTEMPTS) {
					sleepBeforeRetry();
				}
			}
		}
		if (lastFailure != null) {
			throw new ApiException(502, "AI question generation service is unavailable, please try again");
		}

		JsonNode root;
		try {
			root = objectMapper.readTree(rawResponse);
		} catch (Exception e) {
			log.warn("Failed to read Gemini MCQ generation response: {}", e.getMessage());
			throw new ApiException(502, "AI question generation service returned an unexpected response");
		}

		String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
		if (StringUtils.isBlank(text)) {
			throw new ApiException(502, "AI question generation service returned an empty response");
		}

		List<GeneratedMcqItem> items;
		try {
			items = objectMapper.readValue(text, objectMapper.getTypeFactory().constructCollectionType(List.class, GeneratedMcqItem.class));
		} catch (Exception e) {
			log.warn("Failed to parse Gemini MCQ generation JSON: {}", e.getMessage());
			throw new ApiException(502, "AI question generation service returned an unexpected response");
		}

		Long companyId = getLoggedInUserDetails().getCompanyId();
		List<McqQuestionResDTO> created = new ArrayList<>();
		for (GeneratedMcqItem item : items) {
			if (item.questionText == null || item.options == null || item.options.size() < 2) continue;
			McqQuestion question = new McqQuestion()
				.setCompanyId(companyId)
				.setQuestionText(item.questionText)
				.setSkillTag(req.getSkillTag())
				.setDifficulty(StringUtils.defaultIfBlank(item.difficulty, "MEDIUM"))
				.setStatus("DRAFT")
				.setSource("AI_GENERATED");
			for (int i = 0; i < item.options.size(); i++) {
				question.getOptions().add(new McqOptionItem()
					.setOptionKey(String.valueOf((char) ('A' + i)))
					.setOptionText(item.options.get(i))
					.setCorrect(i == item.correctOptionIndex)
					.setDisplayOrder(i));
			}
			question = createEntity(question);
			created.add(new McqQuestionResDTO(question));
		}

		return success(created);
	}

	private Map<String, Object> buildGeminiRequest(McqGenerateQuestionsReqDto req, int count) {
		Map<String, Object> content = Map.of("parts", List.of(Map.of("text", buildPrompt(req, count))));

		Map<String, Object> itemProperties = new LinkedHashMap<>();
		itemProperties.put("questionText", Map.of("type", "STRING"));
		itemProperties.put("options", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")));
		itemProperties.put("correctOptionIndex", Map.of("type", "INTEGER"));
		itemProperties.put("difficulty", Map.of("type", "STRING"));

		Map<String, Object> itemSchema = Map.of(
			"type", "OBJECT",
			"properties", itemProperties,
			"required", List.of("questionText", "options", "correctOptionIndex")
		);

		Map<String, Object> arraySchema = Map.of("type", "ARRAY", "items", itemSchema);

		Map<String, Object> generationConfig = Map.of(
			"responseMimeType", "application/json",
			"responseSchema", arraySchema
		);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(content));
		body.put("generationConfig", generationConfig);
		return body;
	}

	private String buildPrompt(McqGenerateQuestionsReqDto req, int count) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are helping a recruiter build an interview screening test. Generate ")
			.append(count).append(" multiple-choice questions as a JSON array matching the given schema.\n\n")
			.append("Topic/skill: ").append(req.getSkillTag()).append("\n");
		if (StringUtils.isNotBlank(req.getDifficulty())) {
			sb.append("Difficulty: ").append(req.getDifficulty()).append("\n");
		}
		sb.append("\nRules:\n")
			.append("- Each question must have exactly 4 plausible options, with exactly one correct.\n")
			.append("- correctOptionIndex is the 0-based index of the correct option within that question's options array.\n")
			.append("- difficulty must be EASY, MEDIUM, or HARD.\n")
			.append("- Questions must be realistic, professional, and specific to the given topic/skill - suitable for screening a real job candidate.\n")
			.append("- Do not repeat the same question twice.");
		return sb.toString();
	}

	private void sleepBeforeRetry() {
		try {
			Thread.sleep(RETRY_BACKOFF_MILLIS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void requireStaff() {
		MyUserDetail me = getLoggedInUserDetails();
		if (me == null) throw new ForbiddenException("Access denied");
		boolean staff = me.getAuthorities().stream().anyMatch(a ->
			STAFF_AUTHORITY.equals(a.getAuthority()) || "SUPER_ADMIN".equals(a.getAuthority()));
		if (!staff) throw new ForbiddenException("Only recruiters/admins may generate AI questions");
	}

	private Response<McqQuestionResDTO> success(List<McqQuestionResDTO> created) {
		Response<McqQuestionResDTO> response = new Response<>();
		response.setCode(201);
		response.setSuccess(true);
		response.setMessage(created.size() + " draft question(s) generated");
		response.setList(created);
		return response;
	}

	private static RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(30000);
		return new RestTemplate(factory);
	}

	private static class GeneratedMcqItem {
		public String questionText;
		public List<String> options;
		public int correctOptionIndex;
		public String difficulty;
	}
}
