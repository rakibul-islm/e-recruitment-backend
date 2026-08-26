package com.bd.erecruitment.seed.seeder;

import com.bd.erecruitment.entity.ArchiveConfig;
import com.bd.erecruitment.repository.ArchiveConfigRepo;
import com.bd.erecruitment.seed.DataSeeder;
import com.bd.erecruitment.seed.data.ArchiveConfigData;
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
public class ArchiveConfigSeeder implements DataSeeder {

	private final ArchiveConfigRepo archiveConfigRepo;

	@Transactional
	@Override
	public void seed() {
		Date now = new Date();
		List<ArchiveConfig> toInsert = ArchiveConfigData.get().stream()
			.filter(def -> archiveConfigRepo.findBySourceTableAndDeleted(def.sourceTable(), false).isEmpty())
			.map(def -> {
				ArchiveConfig c = new ArchiveConfig();
				c.setSourceTable(def.sourceTable())
					.setArchiveSchema(def.archiveSchema())
					.setArchiveTable(def.archiveTable())
					.setDateColumn(def.dateColumn())
					.setRetentionDays(def.retentionDays())
					.setEnabled(true)
					.setDescription(def.description())
					.setCreatedBy("system").setCreatedOn(now)
					.setUpdatedBy("system").setUpdatedOn(now)
					.setDeleted(false);
				return c;
			})
			.toList();

		if (toInsert.isEmpty()) {
			log.info("[ArchiveConfigSeeder] already seeded, skipping");
			return;
		}
		archiveConfigRepo.saveAll(toInsert);
		log.info("[ArchiveConfigSeeder] inserted {} archive config entries", toInsert.size());
	}
}
