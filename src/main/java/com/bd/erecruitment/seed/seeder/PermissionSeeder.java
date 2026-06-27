package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.Permission;
import com.bd.erecruitment.repository.PermissionRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.PermissionData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class PermissionSeeder implements DataSeeder {

	private final PermissionRepo permissionRepo;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		List<Permission> toInsert = PermissionData.get().stream()
			.filter(def -> permissionRepo.findByAuthority(def.authority()) == null)
			.map(def -> {
				Permission p = new Permission();
				p.setName(def.name())
					.setAuthority(def.authority())
					.setModule(def.module())
					.setRouteName(def.routeName())
					.setCreatedBy("system").setCreatedOn(now)
					.setUpdatedBy("system").setUpdatedOn(now)
					.setDeleted(false);
				return p;
			})
			.toList();

		if (toInsert.isEmpty()) {
			log.info("[PermissionSeeder] already seeded, skipping");
			return;
		}
		permissionRepo.saveAll(toInsert);
		log.info("[PermissionSeeder] inserted {} permissions", toInsert.size());
	}
}
