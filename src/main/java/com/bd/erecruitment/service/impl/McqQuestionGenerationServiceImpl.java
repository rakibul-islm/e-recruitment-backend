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

@Slf4j
@Service
public class McqQuestionGenerationServiceImpl extends AbstractBaseService<McqQuestion> {

	private static final String STAFF_AUTHORITY = "job-circular:write";
	private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
	private static final int MAX_ATTEMPTS = 3;
	private static final long RETRY_BACKOFF_MILLIS = 1000;
	private static final int DEFAULT_COUNT = 5;
	private static final int MAX_COUNT = 20;
	private static final int BATCH_SIZE = 5;
	private static final int MAX_EMPTY_BATCHES = 2;
	private static final int HARD_BATCH_SIZE = 3;
	private static final long TOTAL_BUDGET_MILLIS = 80_000;
	private static final long CALL_TIMEOUT_MILLIS = 45_000;
	private static final long MIN_CALL_MILLIS = 8_000;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.ai.gemini.api-key:}")
	private String apiKey;

	@Value("${app.ai.gemini.model:}")
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

		List<GeneratedMcqItem> items = new ArrayList<>();
		int emptyBatches = 0;
		long deadline = System.currentTimeMillis() + TOTAL_BUDGET_MILLIS;
		int perBatch = "HARD".equalsIgnoreCase(req.getDifficulty()) ? HARD_BATCH_SIZE : BATCH_SIZE;
		boolean outOfTime = false;
		while (items.size() < count && emptyBatches < MAX_EMPTY_BATCHES) {
			long remaining = deadline - System.currentTimeMillis();
			if (remaining < MIN_CALL_MILLIS) {
				outOfTime = true;
				break;
			}
			int batchSize = Math.min(perBatch, count - items.size());
			List<GeneratedMcqItem> batch;
			try {
				batch = fetchBatch(url, req, batchSize, items, remaining);
			} catch (ApiException e) {
				if (items.isEmpty()) throw e;
				log.warn("Stopping MCQ generation early with {} of {} questions: {}", items.size(), count, e.getMessage());
				break;
			}
			batch.removeIf(i -> i.questionText == null || i.options == null || i.options.size() < 2);
			if (batch.isEmpty()) {
				emptyBatches++;
			} else {
				items.addAll(batch);
			}
		}
		if (items.size() > count) {
			items = new ArrayList<>(items.subList(0, count));
		}

		Long organizationId = getLoggedInUserDetails().getOrganizationId();
		List<McqQuestionResDTO> created = new ArrayList<>();
		for (GeneratedMcqItem item : items) {
			McqQuestion question = new McqQuestion()
				.setOrganizationId(organizationId)
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

		if (created.isEmpty() && outOfTime) {
			throw new ApiException(504, "AI question generation took too long, please try again or request fewer questions");
		}
		if (created.isEmpty()) {
			log.warn("Gemini MCQ generation produced no valid questions (requested {})", count);
			throw new ApiException(502, "AI question generation returned no valid questions, please try again");
		}

		return success(created);
	}

	private List<GeneratedMcqItem> fetchBatch(String url, McqGenerateQuestionsReqDto req, int batchSize, List<GeneratedMcqItem> existing, long remainingMillis) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildGeminiRequest(req, batchSize, existing), headers);

		String rawResponse = null;
		RestClientException lastFailure = null;
		long deadline = System.currentTimeMillis() + remainingMillis;
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			long callMillis = Math.min(CALL_TIMEOUT_MILLIS, deadline - System.currentTimeMillis());
			if (callMillis < MIN_CALL_MILLIS) break;
			try {
				rawResponse = buildRestTemplate((int) callMillis).exchange(url, HttpMethod.POST, entity, String.class).getBody();
				lastFailure = null;
				break;
			} catch (RestClientException e) {
				lastFailure = e;
				log.warn("Gemini MCQ generation call failed (attempt {}/{}, limit {} ms): {}", attempt, MAX_ATTEMPTS, callMillis, e.getMessage());
				if (isReadTimeout(e)) {
					throw new ApiException(504, "AI question generation took too long, please try again or request fewer questions");
				}
				if (attempt < MAX_ATTEMPTS) {
					sleepBeforeRetry();
				}
			}
		}
		if (lastFailure != null || rawResponse == null) {
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
			log.warn("Gemini MCQ generation returned no text. Response: {}", rawResponse);
			throw new ApiException(502, "AI question generation service returned an empty response");
		}

		try {
			JsonNode parsedRoot = objectMapper.readTree(text.trim());
			JsonNode questions = parsedRoot.isArray() ? parsedRoot : parsedRoot.path("questions");
			List<GeneratedMcqItem> parsed = objectMapper.readerFor(objectMapper.getTypeFactory().constructCollectionType(List.class, GeneratedMcqItem.class)).readValue(questions);
			if (parsed == null || parsed.isEmpty()) {
				log.warn("Gemini MCQ generation returned no items (requested {}). Raw text: {}", batchSize, text);
				return new ArrayList<>();
			}
			return new ArrayList<>(parsed);
		} catch (Exception e) {
			log.warn("Failed to parse Gemini MCQ generation JSON: {}. Raw text: {}", e.getMessage(), text);
			throw new ApiException(502, "AI question generation service returned an unexpected response");
		}
	}

	private Map<String, Object> buildGeminiRequest(McqGenerateQuestionsReqDto req, int count, List<GeneratedMcqItem> existing) {
		Map<String, Object> content = Map.of("parts", List.of(Map.of("text", buildPrompt(req, count, existing))));

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

		// Wrapped in an object: a top-level ARRAY schema makes the model return "[]".
		Map<String, Object> wrapperSchema = Map.of(
			"type", "OBJECT",
			"properties", Map.of("questions", Map.of("type", "ARRAY", "items", itemSchema)),
			"required", List.of("questions")
		);

		Map<String, Object> generationConfig = Map.of(
			"responseMimeType", "application/json",
			"responseSchema", wrapperSchema
		);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(content));
		body.put("generationConfig", generationConfig);
		return body;
	}

	private String buildPrompt(McqGenerateQuestionsReqDto req, int count, List<GeneratedMcqItem> existing) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are helping a recruiter build an interview screening test. Generate ")
			.append(count).append(" multiple-choice questions as JSON matching the given schema (an object with a \"questions\" array).\n\n")
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
		if (!existing.isEmpty()) {
			sb.append("\n\nAlready generated (do not repeat or paraphrase these):\n");
			existing.forEach(i -> sb.append("- ").append(i.questionText).append("\n"));
		}
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

	// A read timeout means the model is still thinking; retrying the same call would only repeat the wait.
	private static boolean isReadTimeout(Throwable e) {
		for (Throwable t = e; t != null; t = t.getCause()) {
			if (t instanceof java.net.SocketTimeoutException) return true;
		}
		return false;
	}

	private static RestTemplate buildRestTemplate(int readTimeoutMillis) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(readTimeoutMillis);
		return new RestTemplate(factory);
	}

	private static class GeneratedMcqItem {
		public String questionText;
		public List<String> options;
		public int correctOptionIndex;
		public String difficulty;
	}
}
