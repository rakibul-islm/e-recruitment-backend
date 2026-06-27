package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Permission;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepo extends ServiceRepository<Permission> {
	Permission findByAuthority(String authority);
	Permission findByRouteName(String routeName);
}
