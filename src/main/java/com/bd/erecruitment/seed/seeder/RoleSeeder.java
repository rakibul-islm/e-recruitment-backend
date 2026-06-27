package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.Permission;
import com.bd.erecruitment.entity.Role;
import com.bd.erecruitment.repository.PermissionRepo;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.RoleData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RoleSeeder implements DataSeeder {

	private final RoleRepo roleRepo;
	private final PermissionRepo permissionRepo;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		int count = 0;

		for (RoleData.RoleDef def : RoleData.get()) {
			if (roleRepo.findByCode(def.code()) != null) continue;

			Set<Permission> permissions = def.authorities().stream()
				.map(permissionRepo::findByAuthority)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

			Role role = new Role();
			role.setName(def.name())
				.setCode(def.code())
				.setDescription(def.description())
				.setPermissions(permissions)
				.setCreatedBy("system").setCreatedOn(now)
				.setUpdatedBy("system").setUpdatedOn(now)
				.setDeleted(false);

			roleRepo.save(role);
			count++;
		}

		if (count == 0) log.info("[RoleSeeder] already seeded, skipping");
		else log.info("[RoleSeeder] inserted {} roles", count);
	}
}
