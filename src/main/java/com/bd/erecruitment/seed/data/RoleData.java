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
				"Registered User",
				"REGISTERED_USER",
				"Default role for self-registered/Google sign-in users: own profile + job browsing only, no administration access",
				List.of(
					"profile:read",
					"profile:write",
					"job-circular:read"
				)
			),
			new RoleDef(
				"Viewer",
				"VIEWER",
				"Read-only access across all modules; no create/update/delete permissions anywhere",
				List.of(
					"user:read",
					"permission:read",
					"role:read",
					"user-group:read",
					"job-circular:read",
					"system-config:read",
					"password-policy:read",
					"exception-log:read"
				)
			),
			new RoleDef(
				"Editor",
				"EDITOR",
				"Read and write access across all modules; no delete permissions anywhere",
				List.of(
					"user:read",
					"user:write",
					"permission:read",
					"permission:write",
					"role:read",
					"role:write",
					"user-group:read",
					"user-group:write",
					"job-circular:read",
					"job-circular:write",
					"password-policy:read",
					"password-policy:write",
					"exception-log:read"
				)
			),
			new RoleDef(
				"Manager",
				"MANAGER",
				"Read, write and delete access across all modules; full non-admin control standalone",
				List.of(
					"user:read",
					"user:write",
					"user:delete",
					"permission:read",
					"permission:write",
					"permission:delete",
					"role:read",
					"role:write",
					"role:delete",
					"user-group:read",
					"user-group:write",
					"user-group:delete",
					"job-circular:read",
					"job-circular:write",
					"job-circular:delete",
					"password-policy:read",
					"password-policy:write",
					"password-policy:delete",
					"exception-log:read",
					"exception-log:delete"
				)
			)
		);
	}
}
