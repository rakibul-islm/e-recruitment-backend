package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.SystemConfig;
import com.bd.erecruitment.repository.SystemConfigRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.SystemConfigData;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SystemConfigSeeder implements DataSeeder {

	private final SystemConfigRepo systemConfigRepo;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		List<SystemConfig> toInsert = SystemConfigData.get().stream()
			.filter(def -> systemConfigRepo.findByConfigKeyAndDeleted(def.key(), false).isEmpty())
			.map(def -> {
				SystemConfig c = new SystemConfig();
				c.setConfigKey(def.key())
					.setConfigValue(def.value())
					.setDescription(def.description())
					.setExpectedValues(def.expectedValues())
					.setCreatedBy("system").setCreatedOn(now)
					.setUpdatedBy("system").setUpdatedOn(now)
					.setDeleted(false);
				return c;
			})
			.toList();

		if (toInsert.isEmpty()) {
			log.info("[SystemConfigSeeder] already seeded, skipping");
			return;
		}
		systemConfigRepo.saveAll(toInsert);
		log.info("[SystemConfigSeeder] inserted {} config entries", toInsert.size());
	}
}
