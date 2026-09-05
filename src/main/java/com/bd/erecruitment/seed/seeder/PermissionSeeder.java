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
		int inserted = 0;
		int restored = 0;

		for (PermissionData.PermissionDef def : PermissionData.get()) {
			Permission existing = permissionRepo.findByAuthority(def.authority());
			if (existing == null) {
				Permission p = new Permission();
				p.setName(def.name())
					.setAuthority(def.authority())
					.setModule(def.module())
					.setRouteName(def.routeName())
					.setCreatedBy("system").setCreatedOn(now)
					.setUpdatedBy("system").setUpdatedOn(now)
					.setDeleted(false);
				permissionRepo.save(p);
				inserted++;
			} else if (existing.isDeleted()) {
				// authority is unique, so a soft-deleted permission can never be re-created through
				// the UI, and findByAuthority ignores the deleted flag, so it would otherwise stay
				// hidden forever instead of ever being re-seeded - heal it back to the canonical
				// definition rather than leaving admins with a permanently missing permission.
				existing.setName(def.name())
					.setModule(def.module())
					.setRouteName(def.routeName())
					.setUpdatedBy("system").setUpdatedOn(now)
					.setDeleted(false);
				permissionRepo.save(existing);
				restored++;
			}
		}

		if (inserted == 0 && restored == 0) log.info("[PermissionSeeder] already seeded, skipping");
		else log.info("[PermissionSeeder] inserted {} permissions, restored {} soft-deleted permissions", inserted, restored);
	}
}
