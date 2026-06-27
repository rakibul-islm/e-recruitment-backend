package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Role;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepo extends ServiceRepository<Role> {
	Role findByCode(String code);
}
