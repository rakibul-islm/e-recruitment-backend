package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.CompanyType;
import com.bd.erecruitment.repository.CompanyTypeRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.CompanyTypeData;
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
public class CompanyTypeSeeder implements DataSeeder {

	private final CompanyTypeRepo companyTypeRepo;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		int count = 0;

		for (String name : CompanyTypeData.get()) {
			if (companyTypeRepo.findFirstByNameIgnoreCaseAndDeleted(name, false).isPresent()) continue;

			CompanyType companyType = new CompanyType();
			companyType.setName(name)
				.setCreatedBy("system").setCreatedOn(now)
				.setUpdatedBy("system").setUpdatedOn(now)
				.setDeleted(false);

			companyTypeRepo.save(companyType);
			count++;
		}

		if (count == 0) log.info("[CompanyTypeSeeder] already seeded, skipping");
		else log.info("[CompanyTypeSeeder] inserted {} company types", count);
	}
}
