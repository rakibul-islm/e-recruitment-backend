package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.JobPostingAiSuggestReqDto;
import com.bd.erecruitment.dto.res.JobPostingAiSuggestResDTO;
import com.bd.erecruitment.exception.ApiException;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Uses Google Gemini's free-tier API to draft job posting content from a job title + whatever
// other fields the recruiter has already filled in. Called through the backend (rather than from
// Angular directly) so the API key never reaches the browser.
@Slf4j
@Service
public class JobPostingAiServiceImpl {

	private static final String STAFF_AUTHORITY = "job-circular:write";
	private static final String GEMINI_URL_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

	private final RestTemplate restTemplate = buildRestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${app.ai.gemini.api-key:}")
	private String apiKey;

	@Value("${app.ai.gemini.model:gemini-3.6-flash}")
	private String model;

	public Response<JobPostingAiSuggestResDTO> suggest(JobPostingAiSuggestReqDto req) {
		requireStaff();

		if (StringUtils.isBlank(req.getJobTitle())) {
			throw new BadRequestException("Job title is required to generate AI suggestions");
		}
		if (StringUtils.isBlank(apiKey)) {
			throw new ApiException(503, "AI suggestions are not configured on this server");
		}

		String url = String.format(GEMINI_URL_TEMPLATE, model, apiKey);
		JsonNode root;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildGeminiRequest(req), headers);
			String rawResponse = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
			root = objectMapper.readTree(rawResponse);
		} catch (RestClientException e) {
			log.warn("Gemini AI suggestion call failed: {}", e.getMessage());
			throw new ApiException(502, "AI suggestion service is unavailable, please try again");
		} catch (Exception e) {
			log.warn("Failed to read Gemini AI response: {}", e.getMessage());
			throw new ApiException(502, "AI suggestion service returned an unexpected response");
		}

		String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText(null);
		if (StringUtils.isBlank(text)) {
			throw new ApiException(502, "AI suggestion service returned an empty response");
		}

		JobPostingAiSuggestResDTO dto;
		try {
			dto = objectMapper.readValue(text, JobPostingAiSuggestResDTO.class);
		} catch (Exception e) {
			log.warn("Failed to parse Gemini AI suggestion JSON: {}", e.getMessage());
			throw new ApiException(502, "AI suggestion service returned an unexpected response");
		}

		return success(dto);
	}

	private Map<String, Object> buildGeminiRequest(JobPostingAiSuggestReqDto req) {
		Map<String, Object> content = Map.of("parts", List.of(Map.of("text", buildPrompt(req))));

		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put("jobRequirement", Map.of("type", "STRING"));
		properties.put("jobResponsibilities", Map.of("type", "STRING"));
		properties.put("otherBenefits", Map.of("type", "STRING"));
		properties.put("skills", Map.of("type", "STRING"));
		properties.put("category", Map.of("type", "STRING"));
		properties.put("experience", Map.of("type", "STRING"));
		properties.put("employmentStatus", Map.of("type", "STRING"));

		Map<String, Object> schema = Map.of(
				"type", "OBJECT",
				"properties", properties,
				"required", List.of("jobRequirement", "jobResponsibilities")
		);

		Map<String, Object> generationConfig = Map.of(
				"responseMimeType", "application/json",
				"responseSchema", schema
		);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("contents", List.of(content));
		body.put("generationConfig", generationConfig);
		return body;
	}

	private String buildPrompt(JobPostingAiSuggestReqDto req) {
		StringBuilder sb = new StringBuilder();
		sb.append("You are helping a recruiter draft a job posting. Based on the context below, generate ")
				.append("job posting content as JSON matching the given schema.\n\n")
				.append("Job title: ").append(req.getJobTitle()).append("\n");
		appendIfPresent(sb, "Company", req.getCompanyName());
		appendIfPresent(sb, "Location", req.getJobLocation());
		appendIfPresent(sb, "Employment type (already set, keep it unchanged)", req.getEmploymentStatus());
		appendIfPresent(sb, "Experience (already set, keep it unchanged)", req.getExperience());
		appendIfPresent(sb, "Category (already set, keep it unchanged)", req.getCategory());
		appendIfPresent(sb, "Skills already listed (already set, keep it unchanged unless it's clearly incomplete)", req.getSkills());

		sb.append("\nRules:\n")
				.append("- jobRequirement and jobResponsibilities must be simple HTML using only <p>, <ul>, <ol>, <li>, <strong> tags (this feeds a rich-text editor).\n")
				.append("- otherBenefits is plain text, 2-4 short bullet-style sentences separated by newlines.\n")
				.append("- skills is a comma-separated list of the key skills/technologies for this role.\n")
				.append("- category is a short job category/department name (e.g. \"Engineering\", \"Sales\").\n")
				.append("- experience is a short phrase (e.g. \"2-4 years\").\n")
				.append("- employmentStatus is a short phrase (e.g. \"Full-time\").\n")
				.append("- For any field marked above as already set, return that same value unchanged.\n")
				.append("- Keep everything realistic, professional, and specific to the job title and context given.");
		return sb.toString();
	}

	private void appendIfPresent(StringBuilder sb, String label, String value) {
		if (StringUtils.isNotBlank(value)) {
			sb.append(label).append(": ").append(value).append("\n");
		}
	}

	private void requireStaff() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof MyUserDetail me)) {
			throw new ForbiddenException("Access denied");
		}
		boolean staff = me.getAuthorities().stream().anyMatch(a ->
				STAFF_AUTHORITY.equals(a.getAuthority()) || "SUPER_ADMIN".equals(a.getAuthority()));
		if (!staff) throw new ForbiddenException("Only recruiters/admins may use AI suggestions");
	}

	private Response<JobPostingAiSuggestResDTO> success(JobPostingAiSuggestResDTO obj) {
		Response<JobPostingAiSuggestResDTO> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage("Generated");
		response.setObj(obj);
		return response;
	}

	private static RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(5000);
		factory.setReadTimeout(30000);
		return new RestTemplate(factory);
	}
}
