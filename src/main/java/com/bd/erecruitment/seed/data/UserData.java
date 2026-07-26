package com.bd.erecruitment.seed.data;

import java.util.List;

public class UserData {

	public record UserDef(String fullName, String email, String password, List<String> roleCodes, List<String> groupNames) {}

	public static List<UserDef> get() {
		return List.of(
			new UserDef(
				"System Admin",
				"admin@e-recruitment.com",
				"a",
				List.of("SUPER_ADMIN"),
				List.of()
			),
			new UserDef(
				"Test User",
				"test@e-recruitment.com",
				"t",
				List.of(),
				List.of("Normal User")
			)
		);
	}
}
