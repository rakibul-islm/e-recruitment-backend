package com.bd.erecruitment.backup;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleDriveClientTest {

	@TempDir
	Path tempDir;

	private HttpServer server;
	private String base;
	private final List<String> requests = new ArrayList<>();
	private String tokenReply;
	private int existingFolders;
	private byte[] uploadedBytes;
	private String createdFolderBody;
	private String initBody;

	@BeforeEach
	void startFakeGoogle() throws IOException {
		tokenReply = "{\"access_token\":\"at-1\",\"expires_in\":3600}";
		existingFolders = 0;
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		base = "http://localhost:" + server.getAddress().getPort();

		server.createContext("/token", ex -> {
			requests.add("TOKEN " + body(ex));
			boolean ok = tokenReply.contains("access_token");
			reply(ex, ok ? 200 : 400, tokenReply);
		});
		server.createContext("/drive/files", ex -> {
			String method = ex.getRequestMethod();
			String uri = URLDecoder.decode(ex.getRequestURI().toString(), StandardCharsets.UTF_8);
			requests.add(method + " " + uri + " auth=" + ex.getRequestHeaders().getFirst("Authorization"));
			if (method.equals("GET")) {
				reply(ex, 200, existingFolders > 0 ? "{\"files\":[{\"id\":\"folder-existing\"}]}" : "{\"files\":[]}");
			} else if (method.equals("POST")) {
				createdFolderBody = body(ex);
				reply(ex, 200, "{\"id\":\"folder-new\"}");
			} else {
				ex.sendResponseHeaders(204, -1);
				ex.close();
			}
		});
		server.createContext("/upload/files", ex -> {
			requests.add(ex.getRequestMethod() + " " + ex.getRequestURI() + " type=" + ex.getRequestHeaders().getFirst("X-Upload-Content-Type")
					+ " length=" + ex.getRequestHeaders().getFirst("X-Upload-Content-Length"));
			initBody = body(ex);
			ex.getResponseHeaders().add("Location", base + "/session/abc");
			ex.sendResponseHeaders(200, -1);
			ex.close();
		});
		server.createContext("/session/abc", ex -> {
			requests.add("PUT /session/abc");
			uploadedBytes = ex.getRequestBody().readAllBytes();
			reply(ex, 200, "{\"id\":\"drive-file-1\"}");
		});
		server.start();
	}

	@AfterEach
	void stopFakeGoogle() {
		server.stop(0);
	}

	private GoogleDriveClient client(String refreshToken) {
		return new GoogleDriveClient("cid", "secret", refreshToken, "E-Recruitment Backups", base + "/token", base + "/drive", base + "/upload");
	}

	@Test
	void uploadsToANewlyCreatedFolderAndReusesItForTheNextUpload() throws Exception {
		Path file = tempDir.resolve("backup.zip");
		Files.write(file, new byte[]{1, 2, 3, 4, 5});
		GoogleDriveClient client = client("refresh-1");

		assertEquals("drive-file-1", client.upload(file, "backup.zip"));
		assertEquals("drive-file-1", client.upload(file, "backup.zip"));

		assertEquals(List.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5), List.of(uploadedBytes[0], uploadedBytes[1], uploadedBytes[2], uploadedBytes[3], uploadedBytes[4]));
		assertEquals(1, requests.stream().filter(r -> r.startsWith("TOKEN")).count());
		assertTrue(requests.get(0).contains("refresh_token=refresh-1") && requests.get(0).contains("grant_type=refresh_token"));
		assertEquals(1, requests.stream().filter(r -> r.startsWith("GET /drive/files")).count());
		assertEquals(1, requests.stream().filter(r -> r.startsWith("POST /drive/files")).count());
		assertTrue(createdFolderBody.contains("application/vnd.google-apps.folder") && createdFolderBody.contains("E-Recruitment Backups"));
		assertTrue(initBody.contains("\"name\":\"backup.zip\"") && initBody.contains("folder-new"));
		assertTrue(requests.stream().anyMatch(r -> r.startsWith("POST /upload/files?uploadType=resumable type=application/zip length=5")));
		assertTrue(requests.stream().anyMatch(r -> r.contains("auth=Bearer at-1")));
	}

	@Test
	void usesAnExistingFolderInsteadOfCreatingAnother() throws Exception {
		existingFolders = 1;
		Path file = tempDir.resolve("backup.zip");
		Files.write(file, new byte[]{9});

		client("refresh-1").upload(file, "backup.zip");

		assertFalse(requests.stream().anyMatch(r -> r.startsWith("POST /drive/files")));
		assertTrue(initBody.contains("folder-existing"));
	}

	@Test
	void deletesByFileId() {
		client("refresh-1").delete("drive-file-1");

		assertTrue(requests.stream().anyMatch(r -> r.startsWith("DELETE /drive/files/drive-file-1")));
	}

	@Test
	void explainsARevokedRefreshToken() throws Exception {
		tokenReply = "{\"error\":\"invalid_grant\",\"error_description\":\"Token has been expired or revoked.\"}";
		Path file = tempDir.resolve("backup.zip");
		Files.write(file, new byte[]{1});

		GoogleDriveClient.DriveException e = assertThrows(GoogleDriveClient.DriveException.class, () -> client("old-token").upload(file, "backup.zip"));

		assertTrue(e.getMessage().contains("DRIVE_REFRESH_TOKEN"));
		assertTrue(e.getMessage().contains("expired or revoked"));
	}

	@Test
	void isNotConfiguredWithoutARefreshToken() {
		assertFalse(client("").isConfigured());
		assertTrue(client("x").isConfigured());
	}

	private String body(HttpExchange ex) throws IOException {
		return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
	}

	private void reply(HttpExchange ex, int status, String json) throws IOException {
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		ex.getResponseHeaders().add("Content-Type", "application/json");
		ex.sendResponseHeaders(status, bytes.length);
		ex.getResponseBody().write(bytes);
		ex.close();
	}
}
