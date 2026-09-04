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
				"Default role for self-registered/Google sign-in users: candidate job portal access - own profile, own generated CVs, job browsing and applying, no administration access",
				List.of(
					"profile:read",
					"profile:write",
					"candidate-profile:read",
					"candidate-profile:write",
					"application:read",
					"application:write",
					"interview:read",
					"offer:read",
					"offer:write",
					"onboarding-task:read",
					"onboarding-task:write",
					"saved-job:read",
					"saved-job:write",
					"job-alert:read",
					"job-alert:write",
					"job-alert:delete"
				)
			),
			new RoleDef(
				"Recruiter",
				"RECRUITER",
				"HR/recruiting staff: manage job postings and their own company, review and progress candidate applications through the hiring pipeline",
				List.of(
					"job-circular:read",
					"job-circular:write",
					"job-circular:delete",
					"company:read",
					"company:write",
					"application:read",
					"application:write",
					"interview:read",
					"interview:write",
					"offer:read",
					"offer:write",
					"onboarding-task:read",
					"onboarding-task:write",
					"analytics:read"
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
					"company:read",
					"application:read",
					"interview:read",
					"offer:read",
					"onboarding-task:read",
					"analytics:read",
					"system-config:read",
					"password-policy:read",
					"exception-log:read",
					"audit-log:read",
					"session:read"
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
					"recruiter-application:read",
					"job-circular:read",
					"job-circular:write",
					"company:read",
					"company:write",
					"application:read",
					"application:write",
					"interview:read",
					"interview:write",
					"offer:read",
					"offer:write",
					"onboarding-task:read",
					"onboarding-task:write",
					"analytics:read",
					"password-policy:read",
					"password-policy:write",
					"exception-log:read",
					"audit-log:read",
					"session:read"
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
					"recruiter-application:read",
					"recruiter-application:write",
					"job-circular:read",
					"job-circular:write",
					"job-circular:delete",
					"company:read",
					"company:write",
					"company:delete",
					"application:read",
					"application:write",
					"interview:read",
					"interview:write",
					"offer:read",
					"offer:write",
					"onboarding-task:read",
					"onboarding-task:write",
					"analytics:read",
					"password-policy:read",
					"password-policy:write",
					"password-policy:delete",
					"exception-log:read",
					"exception-log:delete",
					"audit-log:read",
					"session:read",
					"session:delete"
				)
			)
		);
	}
}
