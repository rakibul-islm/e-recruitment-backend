package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.PasswordPolicy;
import com.bd.erecruitment.repository.PasswordPolicyRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.PasswordPolicyData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class PasswordPolicySeeder implements DataSeeder {

	private final PasswordPolicyRepo passwordPolicyRepo;

	@Transactional
	@Override
	public void seed() {
		if (passwordPolicyRepo.count() > 0) {
			log.info("[PasswordPolicySeeder] already seeded, skipping");
			return;
		}

		PasswordPolicyData.PolicyDef def = PasswordPolicyData.getDefault();
		Date now = new Date();
		PasswordPolicy policy = new PasswordPolicy()
			.setMinLength(def.minLength())
			.setMaxLength(def.maxLength())
			.setRequireUppercase(def.requireUppercase())
			.setRequireLowercase(def.requireLowercase())
			.setRequireDigit(def.requireDigit())
			.setRequireSpecialChar(def.requireSpecialChar())
			.setDisallowUserInfoInPassword(def.disallowUserInfoInPassword());
		policy.setCreatedBy("system");
		policy.setCreatedOn(now);
		policy.setUpdatedBy("system");
		policy.setUpdatedOn(now);
		policy.setDeleted(false);

		passwordPolicyRepo.save(policy);
		log.info("[PasswordPolicySeeder] inserted default password policy");
	}
}
