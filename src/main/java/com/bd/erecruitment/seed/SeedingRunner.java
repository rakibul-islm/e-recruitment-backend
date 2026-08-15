package com.bd.erecruitment.seed;

import com.bd.erecruitment.exception.ExceptionLogWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeedingRunner implements ApplicationRunner {

	private final List<DataSeeder> seeders;
	private final ExceptionLogWriter exceptionLogWriter;

	@Override
	public void run(ApplicationArguments args) {
		log.info("========== Starting data seeding ==========");
		List<DataSeeder> ordered = seeders.stream()
			.sorted(AnnotationAwareOrderComparator.INSTANCE)
			.toList();
		ordered.forEach(seeder -> {
			try {
				seeder.seed();
			} catch (Exception e) {
				exceptionLogWriter.log(e, 0, e.getMessage(), "seed:" + seeder.getClass().getSimpleName());
			}
		});
		log.info("========== Data seeding complete ==========");
	}
}
