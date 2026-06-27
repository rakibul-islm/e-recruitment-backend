package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.Role;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.entity.UserGroup;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.repository.UserGroupRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.UserData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@Order(4)
@RequiredArgsConstructor
public class UserSeeder implements DataSeeder {

	private final UserRepo userRepo;
	private final RoleRepo roleRepo;
	private final UserGroupRepo userGroupRepo;
	private final BCryptPasswordEncoder encoder;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		int count = 0;

		for (UserData.UserDef def : UserData.get()) {
			if (userRepo.findByUsername(def.username()) != null) continue;

			Set<Role> roles = def.roleCodes().stream()
				.map(roleRepo::findByCode)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

			Set<UserGroup> groups = def.groupNames().stream()
				.map(name -> userGroupRepo.findByNameAndDeletedFalse(name).orElse(null))
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

			User user = new User();
			user.setFullName(def.fullName())
				.setUsername(def.username())
				.setEmail(def.email())
				.setPassword(encoder.encode(def.password()))
				.setActive(true)
				.setLocked(false)
				.setExpiryDate(yearFromNow(50))
				.setRoles(roles)
				.setUserGroups(groups)
				.setCreatedBy("system").setCreatedOn(now)
				.setUpdatedBy("system").setUpdatedOn(now)
				.setDeleted(false);

			userRepo.save(user);
			log.info("[UserSeeder] created user '{}'", def.username());
			count++;
		}

		if (count == 0) log.info("[UserSeeder] already seeded, skipping");
	}

	private Date yearFromNow(int years) {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, years);
		return cal.getTime();
	}
}
