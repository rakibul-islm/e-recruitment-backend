package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.Role;
import com.bd.erecruitment.entity.UserGroup;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.repository.UserGroupRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.UserGroupData;
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
@Order(3)
@RequiredArgsConstructor
public class UserGroupSeeder implements DataSeeder {

	private final UserGroupRepo userGroupRepo;
	private final RoleRepo roleRepo;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		int count = 0;

		for (UserGroupData.UserGroupDef def : UserGroupData.get()) {
			if (userGroupRepo.findByNameAndDeletedFalse(def.name()).isPresent()) continue;

			Set<Role> roles = def.roleCodes().stream()
				.map(roleRepo::findByCode)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

			UserGroup group = new UserGroup();
			group.setName(def.name())
				.setDescription(def.description())
				.setRoles(roles)
				.setCreatedBy("system").setCreatedOn(now)
				.setUpdatedBy("system").setUpdatedOn(now)
				.setDeleted(false);

			userGroupRepo.save(group);
			log.info("[UserGroupSeeder] created group '{}'", def.name());
			count++;
		}

		if (count == 0) log.info("[UserGroupSeeder] already seeded, skipping");
	}
}
