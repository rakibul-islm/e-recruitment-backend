package com.bd.erecruitment.seed.data;

import java.util.List;

public class RoleData {

	public record RoleDef(String name, String code, String description, List<String> authorities) {}

	public static List<RoleDef> get() {
		return List.of(
			new RoleDef(
				"Super Admin",
				"SUPER_ADMIN",
				"Unrestricted system access",
				List.of("SUPER_ADMIN")
			),
			new RoleDef(
				"Normal User",
				"NORMAL_USER",
				"Read-only access across all modules",
				List.of(
					"user:read",
					"permission:read",
					"role:read",
					"user-group:read",
					"job-circular:read"
				)
			),
			new RoleDef(
				"Normal User Write",
				"NORMAL_USER_WRITE",
				"Write access across all modules",
				List.of(
					"user:write",
					"permission:write",
					"role:write",
					"user-group:write",
					"job-circular:write"
				)
			),
			new RoleDef(
				"Normal User Delete",
				"NORMAL_USER_DELETE",
				"Delete access across all modules",
				List.of(
					"user:delete",
					"permission:delete",
					"role:delete",
					"user-group:delete",
					"job-circular:delete"
				)
			)
		);
	}
}
