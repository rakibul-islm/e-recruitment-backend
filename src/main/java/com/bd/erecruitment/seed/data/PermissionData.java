package com.bd.erecruitment.seed.data;

import java.util.List;

public class PermissionData {

	public record PermissionDef(String name, String authority, String module, String routeName) {}

	public static List<PermissionDef> get() {
		return List.of(
			new PermissionDef("Super Admin",            "SUPER_ADMIN",          "SYSTEM",           null),

			new PermissionDef("View Users",             "user:read",            "USER_MANAGEMENT",  "user-list"),
			new PermissionDef("Manage Users",           "user:write",           "USER_MANAGEMENT",  "user-manage"),
			new PermissionDef("Edit User Email",        "user:email-write",     "USER_MANAGEMENT",  "user-email-edit"),
			new PermissionDef("Delete Users",           "user:delete",          "USER_MANAGEMENT",  "user-delete"),

			// routeName left null: profile access is unconditional in the account menu, not admin-menu-gated.
			new PermissionDef("View My Profile",        "profile:read",         "PROFILE",          null),
			new PermissionDef("Update My Profile",      "profile:write",        "PROFILE",          null),

			new PermissionDef("View Permissions",       "permission:read",      "ACCESS_CONTROL",   "permission-list"),
			new PermissionDef("Manage Permissions",     "permission:write",     "ACCESS_CONTROL",   "permission-manage"),
			new PermissionDef("Delete Permissions",     "permission:delete",    "ACCESS_CONTROL",   "permission-delete"),

			new PermissionDef("View Roles",             "role:read",            "ACCESS_CONTROL",   "role-list"),
			new PermissionDef("Manage Roles",           "role:write",           "ACCESS_CONTROL",   "role-manage"),
			new PermissionDef("Delete Roles",           "role:delete",          "ACCESS_CONTROL",   "role-delete"),

			new PermissionDef("View User Groups",       "user-group:read",      "ACCESS_CONTROL",   "user-group-list"),
			new PermissionDef("Manage User Groups",     "user-group:write",     "ACCESS_CONTROL",   "user-group-manage"),
			new PermissionDef("Delete User Groups",     "user-group:delete",    "ACCESS_CONTROL",   "user-group-delete"),

			new PermissionDef("View Recruiter Applications",            "recruiter-application:read",  "USER_MANAGEMENT", "recruiter-application-list"),
			new PermissionDef("Approve/Reject Recruiter Applications",  "recruiter-application:write", "USER_MANAGEMENT", "recruiter-application-manage"),

			new PermissionDef("View Job Circulars",     "job-circular:read",    "JOB_MANAGEMENT",   "job-circular-list"),
			new PermissionDef("Manage Job Circulars",   "job-circular:write",   "JOB_MANAGEMENT",   "job-circular-manage"),
			new PermissionDef("Delete Job Circulars",   "job-circular:delete",  "JOB_MANAGEMENT",   null),

			new PermissionDef("View Companies",         "company:read",         "JOB_MANAGEMENT",   "company-list"),
			new PermissionDef("Manage Companies",       "company:write",        "JOB_MANAGEMENT",   "company-manage"),
			new PermissionDef("Delete Companies",       "company:delete",       "JOB_MANAGEMENT",   null),

			// Self-service only (own profile/CVs) - unconditional for any authenticated account, see PermissionInterceptor.ALWAYS_ALLOWED.
			new PermissionDef("View My Candidate Profile",   "candidate-profile:read",  "PROFILE", null),
			new PermissionDef("Update My Candidate Profile", "candidate-profile:write", "PROFILE", null),

			// routeName left null on write: both candidates (apply) and staff (status change) hold
			// application:write, so it can't distinguish them for Angular route gating - the recruiter
			// application-management list route instead gates on job-circular-manage (staff-only), the
			// same "is staff" proxy ApplicationServiceImpl uses server-side.
			new PermissionDef("View Applications",      "application:read",     "JOB_MANAGEMENT",   "application-list"),
			new PermissionDef("Manage Applications",    "application:write",    "JOB_MANAGEMENT",   null),

			new PermissionDef("View Interviews",        "interview:read",       "JOB_MANAGEMENT",   "interview-list"),
			new PermissionDef("Manage Interviews",      "interview:write",      "JOB_MANAGEMENT",   "interview-manage"),

			new PermissionDef("View Offers",            "offer:read",           "JOB_MANAGEMENT",   "offer-list"),
			new PermissionDef("Manage Offers",          "offer:write",          "JOB_MANAGEMENT",   "offer-manage"),

			new PermissionDef("View Onboarding Tasks",     "onboarding-task:read",  "JOB_MANAGEMENT", "onboarding-task-list"),
			new PermissionDef("Manage Onboarding Tasks",   "onboarding-task:write", "JOB_MANAGEMENT", null),

			new PermissionDef("View Recruitment Analytics", "analytics:read",     "JOB_MANAGEMENT",   "analytics-list"),

			// Self-service only (own saved jobs/alerts) - unconditional for any authenticated account,
			// same as candidate-profile:* - see PermissionInterceptor.ALWAYS_ALLOWED.
			new PermissionDef("Manage My Saved Jobs",   "saved-job:read",       "PROFILE",           null),
			new PermissionDef("Save/Unsave Jobs",       "saved-job:write",      "PROFILE",           null),
			new PermissionDef("Manage My Job Alerts",   "job-alert:read",       "PROFILE",           null),
			new PermissionDef("Create/Update Job Alerts", "job-alert:write",    "PROFILE",           null),
			new PermissionDef("Delete Job Alerts",      "job-alert:delete",     "PROFILE",           null),

			new PermissionDef("View System Config",     "system-config:read",   "SYSTEM_CONFIG",    "system-config-list"),
			new PermissionDef("Manage System Config",   "system-config:write",  "SYSTEM_CONFIG",    "system-config-manage"),
			new PermissionDef("Edit System Config Description", "system-config:description-write", "SYSTEM_CONFIG", "system-config-description-edit"),
			new PermissionDef("Delete System Config",   "system-config:delete", "SYSTEM_CONFIG",    null),

			new PermissionDef("View Password Policy",   "password-policy:read",   "SYSTEM_CONFIG",  "password-policy-list"),
			new PermissionDef("Manage Password Policy",  "password-policy:write",  "SYSTEM_CONFIG",  "password-policy-manage"),
			new PermissionDef("Delete Password Policy",  "password-policy:delete", "SYSTEM_CONFIG",  null),

			new PermissionDef("View Exception Logs",    "exception-log:read",   "SYSTEM_CONFIG",  "exception-log-list"),
			new PermissionDef("Delete Exception Logs",  "exception-log:delete", "SYSTEM_CONFIG",  "exception-log-delete"),

			// No write/delete counterpart: the audit trail is append-only, enforced at the controller (501).
			new PermissionDef("View Audit Logs",        "audit-log:read",       "SYSTEM_CONFIG",  "audit-log-list"),

			new PermissionDef("View Archive Config",    "archive-config:read",   "SYSTEM_CONFIG",  "archive-config-list"),
			new PermissionDef("Manage Archive Config",  "archive-config:write",  "SYSTEM_CONFIG",  "archive-config-manage"),
			new PermissionDef("Delete Archive Config",  "archive-config:delete", "SYSTEM_CONFIG",  "archive-config-delete"),

			new PermissionDef("View Sessions",          "session:read",         "SESSION_MANAGEMENT", "session-list"),
			new PermissionDef("Force Logout Sessions",  "session:delete",       "SESSION_MANAGEMENT", "session-delete")
		);
	}
}
