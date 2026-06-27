package com.bd.erecruitment.seed.data;

import java.util.List;

public class UserData {

	public record UserDef(String fullName, String username, String email, String password, List<String> roleCodes, List<String> groupNames) {}

	public static List<UserDef> get() {
		return List.of(
			new UserDef(
				"System Admin",
				"admin",
				"admin@e-recruitment.com",
				"admin@2025",
				List.of("SUPER_ADMIN"),
				List.of()
			),
			new UserDef(
				"Test User",
				"testuser",
				"testuser@e-recruitment.com",
				"testuser@2025",
				List.of(),
				List.of("Normal User")
			)
		);
	}
}
