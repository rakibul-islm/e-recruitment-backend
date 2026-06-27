package com.bd.erecruitment.seed.data;

import java.util.List;

public class PermissionData {

	public record PermissionDef(String name, String authority, String module, String routeName) {}

	public static List<PermissionDef> get() {
		return List.of(
			// System
			new PermissionDef("Super Admin",            "SUPER_ADMIN",          "SYSTEM",           null),

			// User Management
			new PermissionDef("View Users",             "user:read",            "USER_MANAGEMENT",  "user-list"),
			new PermissionDef("Manage Users",           "user:write",           "USER_MANAGEMENT",  null),
			new PermissionDef("Delete Users",           "user:delete",          "USER_MANAGEMENT",  null),

			// Access Control
			new PermissionDef("View Permissions",       "permission:read",      "ACCESS_CONTROL",   "permission-list"),
			new PermissionDef("Manage Permissions",     "permission:write",     "ACCESS_CONTROL",   null),
			new PermissionDef("Delete Permissions",     "permission:delete",    "ACCESS_CONTROL",   null),

			new PermissionDef("View Roles",             "role:read",            "ACCESS_CONTROL",   "role-list"),
			new PermissionDef("Manage Roles",           "role:write",           "ACCESS_CONTROL",   null),
			new PermissionDef("Delete Roles",           "role:delete",          "ACCESS_CONTROL",   null),

			new PermissionDef("View User Groups",       "user-group:read",      "ACCESS_CONTROL",   "user-group-list"),
			new PermissionDef("Manage User Groups",     "user-group:write",     "ACCESS_CONTROL",   null),
			new PermissionDef("Delete User Groups",     "user-group:delete",    "ACCESS_CONTROL",   null),

			// Job Management
			new PermissionDef("View Job Circulars",     "job-circular:read",    "JOB_MANAGEMENT",   "job-circular-list"),
			new PermissionDef("Manage Job Circulars",   "job-circular:write",   "JOB_MANAGEMENT",   null),
			new PermissionDef("Delete Job Circulars",   "job-circular:delete",  "JOB_MANAGEMENT",   null)
		);
	}
}
