package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.OrganizationType;
import com.bd.erecruitment.repository.OrganizationTypeRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.OrganizationTypeData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@Order(5)
@RequiredArgsConstructor
public class OrganizationTypeSeeder implements DataSeeder {

	private final OrganizationTypeRepo organizationTypeRepo;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		int count = 0;

		for (String name : OrganizationTypeData.get()) {
			if (organizationTypeRepo.findFirstByNameIgnoreCaseAndDeleted(name, false).isPresent()) continue;

			OrganizationType organizationType = new OrganizationType();
			organizationType.setName(name)
				.setCreatedBy("system").setCreatedOn(now)
				.setUpdatedBy("system").setUpdatedOn(now)
				.setDeleted(false);

			organizationTypeRepo.save(organizationType);
			count++;
		}

		if (count == 0) log.info("[OrganizationTypeSeeder] already seeded, skipping");
		else log.info("[OrganizationTypeSeeder] inserted {} organization types", count);
	}
}
