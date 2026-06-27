package com.bd.erecruitment.seed.data;

import java.util.List;

public class UserGroupData {

	public record UserGroupDef(String name, String description, List<String> roleCodes) {}

	public static List<UserGroupDef> get() {
		return List.of(
			new UserGroupDef(
				"Normal User",
				"Standard read-only access across all modules",
				List.of("NORMAL_USER")
			)
		);
	}
}
