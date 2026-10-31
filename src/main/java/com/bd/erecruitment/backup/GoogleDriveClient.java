package com.bd.erecruitment.backup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GoogleDriveClient {

	public static class DriveException extends RuntimeException {
		public DriveException(String message) {
			super(message);
		}
	}

	private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
	private static final String ZIP_MIME = "application/zip";

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
	private final ObjectMapper mapper = new ObjectMapper();

	private final String clientId;
	private final String clientSecret;
	private final String refreshToken;
	private final String folderName;
	private final String tokenUrl;
	private final String apiUrl;
	private final String uploadUrl;

	private String accessToken;
	private Instant accessTokenExpiry = Instant.EPOCH;
	private String folderId;

	public GoogleDriveClient(@Value("${app.mail.gmail.client-id:}") String clientId,
							 @Value("${app.mail.gmail.client-secret:}") String clientSecret,
							 @Value("${app.drive.refresh-token:}") String refreshToken,
							 @Value("${app.drive.folder-name:E-Recruitment Backups}") String folderName,
							 @Value("${app.drive.token-url:https://oauth2.googleapis.com/token}") String tokenUrl,
							 @Value("${app.drive.api-url:https://www.googleapis.com/drive/v3}") String apiUrl,
							 @Value("${app.drive.upload-url:https://www.googleapis.com/upload/drive/v3}") String uploadUrl) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.refreshToken = refreshToken;
		this.folderName = folderName;
		this.tokenUrl = tokenUrl;
		this.apiUrl = apiUrl;
		this.uploadUrl = uploadUrl;
	}

	public boolean isConfigured() {
		return StringUtils.isNoneBlank(clientId, clientSecret, refreshToken);
	}

	public String upload(Path file, String name) {
		try {
			String token = accessToken();
			String folder = folderId(token);

			String metadata = mapper.writeValueAsString(Map.of("name", name, "parents", List.of(folder)));
			HttpRequest init = HttpRequest.newBuilder(URI.create(uploadUrl + "/files?uploadType=resumable"))
					.header("Authorization", "Bearer " + token)
					.header("Content-Type", "application/json; charset=UTF-8")
					.header("X-Upload-Content-Type", ZIP_MIME)
					.header("X-Upload-Content-Length", String.valueOf(Files.size(file)))
					.timeout(Duration.ofSeconds(30))
					.POST(HttpRequest.BodyPublishers.ofString(metadata))
					.build();
			HttpResponse<String> initResponse = send(init);
			if (initResponse.statusCode() == 404) forgetFolder();
			requireOk(initResponse, "Starting the Drive upload");
			String session = initResponse.headers().firstValue("Location").orElseThrow(() -> new DriveException("Drive did not return an upload location"));

			HttpRequest put = HttpRequest.newBuilder(URI.create(session))
					.header("Content-Type", ZIP_MIME)
					.timeout(Duration.ofMinutes(30))
					.PUT(HttpRequest.BodyPublishers.ofFile(file))
					.build();
			HttpResponse<String> putResponse = send(put);
			requireOk(putResponse, "Uploading the file to Drive");

			String id = mapper.readTree(putResponse.body()).path("id").asText();
			if (id.isBlank()) throw new DriveException("Drive accepted the upload but returned no file id");
			return id;
		} catch (IOException e) {
			throw new DriveException("Could not reach Google Drive: " + e.getMessage());
		}
	}

	public void delete(String fileId) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(apiUrl + "/files/" + encode(fileId)))
					.header("Authorization", "Bearer " + accessToken())
					.timeout(Duration.ofSeconds(30))
					.DELETE()
					.build();
			HttpResponse<String> response = send(request);
			if (response.statusCode() != 404) requireOk(response, "Deleting the Drive file");
		} catch (IOException e) {
			throw new DriveException("Could not reach Google Drive: " + e.getMessage());
		}
	}

	private synchronized String accessToken() throws IOException {
		if (!isConfigured()) throw new DriveException("Google Drive is not configured: set DRIVE_REFRESH_TOKEN");
		if (accessToken != null && Instant.now().isBefore(accessTokenExpiry)) return accessToken;

		String form = "client_id=" + encode(clientId) + "&client_secret=" + encode(clientSecret)
				+ "&refresh_token=" + encode(refreshToken) + "&grant_type=refresh_token";
		HttpRequest request = HttpRequest.newBuilder(URI.create(tokenUrl))
				.header("Content-Type", "application/x-www-form-urlencoded")
				.timeout(Duration.ofSeconds(30))
				.POST(HttpRequest.BodyPublishers.ofString(form))
				.build();
		HttpResponse<String> response = send(request);
		if (response.statusCode() / 100 != 2) {
			if (response.body().contains("invalid_grant"))
				throw new DriveException("Google rejected DRIVE_REFRESH_TOKEN (expired or revoked): create a new one and update the environment variable");
			requireOk(response, "Getting a Google access token");
		}
		JsonNode json = mapper.readTree(response.body());
		accessToken = json.path("access_token").asText();
		accessTokenExpiry = Instant.now().plusSeconds(Math.max(60, json.path("expires_in").asLong(3600) - 60));
		return accessToken;
	}

	private synchronized String folderId(String token) throws IOException {
		if (folderId != null) return folderId;

		String query = "name = '" + folderName.replace("\\", "\\\\").replace("'", "\\'") + "' and mimeType = '" + FOLDER_MIME + "' and trashed = false";
		HttpRequest search = HttpRequest.newBuilder(URI.create(apiUrl + "/files?q=" + encode(query) + "&fields=" + encode("files(id)") + "&spaces=drive"))
				.header("Authorization", "Bearer " + token)
				.timeout(Duration.ofSeconds(30))
				.GET()
				.build();
		HttpResponse<String> found = send(search);
		requireOk(found, "Looking for the Drive backup folder");
		JsonNode files = mapper.readTree(found.body()).path("files");
		if (files.isArray() && !files.isEmpty()) {
			folderId = files.get(0).path("id").asText();
			return folderId;
		}

		String body = mapper.writeValueAsString(Map.of("name", folderName, "mimeType", FOLDER_MIME));
		HttpRequest create = HttpRequest.newBuilder(URI.create(apiUrl + "/files?fields=id"))
				.header("Authorization", "Bearer " + token)
				.header("Content-Type", "application/json; charset=UTF-8")
				.timeout(Duration.ofSeconds(30))
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		HttpResponse<String> created = send(create);
		requireOk(created, "Creating the Drive backup folder");
		folderId = mapper.readTree(created.body()).path("id").asText();
		log.info("[GoogleDriveClient] created Drive folder '{}'", folderName);
		return folderId;
	}

	private synchronized void forgetFolder() {
		folderId = null;
	}

	private HttpResponse<String> send(HttpRequest request) throws IOException {
		try {
			return http.send(request, HttpResponse.BodyHandlers.ofString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted while talking to Google Drive");
		}
	}

	private void requireOk(HttpResponse<String> response, String action) {
		if (response.statusCode() / 100 != 2)
			throw new DriveException(action + " failed (HTTP " + response.statusCode() + "): " + StringUtils.abbreviate(response.body(), 300));
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
