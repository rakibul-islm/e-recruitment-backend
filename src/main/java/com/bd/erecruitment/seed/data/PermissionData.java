package com.bd.erecruitment.seed.data;

import java.util.List;

public class PermissionData {

	public record PermissionDef(String name, String authority, String module, String routeName) {}

	public static List<PermissionDef> get() {
		return List.of(
			new PermissionDef("Super Admin",            "SUPER_ADMIN",          "SYSTEM",           null),

			new PermissionDef("View Users",             "user:read",            "USER_MANAGEMENT",  "user-list"),
			new PermissionDef("Manage Users",           "user:write",           "USER_MANAGEMENT",  "user-manage"),
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

			new PermissionDef("View Job Circulars",     "job-circular:read",    "JOB_MANAGEMENT",   "job-circular-list"),
			new PermissionDef("Manage Job Circulars",   "job-circular:write",   "JOB_MANAGEMENT",   null),
			new PermissionDef("Delete Job Circulars",   "job-circular:delete",  "JOB_MANAGEMENT",   null),

			new PermissionDef("View System Config",     "system-config:read",   "SYSTEM_CONFIG",    "system-config-list"),
			new PermissionDef("Manage System Config",   "system-config:write",  "SYSTEM_CONFIG",    "system-config-manage"),
			new PermissionDef("Delete System Config",   "system-config:delete", "SYSTEM_CONFIG",    null),

			new PermissionDef("View Password Policy",   "password-policy:read",   "SYSTEM_CONFIG",  "password-policy-list"),
			new PermissionDef("Manage Password Policy",  "password-policy:write",  "SYSTEM_CONFIG",  "password-policy-manage"),
			new PermissionDef("Delete Password Policy",  "password-policy:delete", "SYSTEM_CONFIG",  null),

			new PermissionDef("View Exception Logs",    "exception-log:read",   "SYSTEM_CONFIG",  "exception-log-list"),
			new PermissionDef("Delete Exception Logs",  "exception-log:delete", "SYSTEM_CONFIG",  "exception-log-delete")
		);
	}
}
