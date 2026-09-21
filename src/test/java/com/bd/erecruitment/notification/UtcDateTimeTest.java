package com.bd.erecruitment.notification;

import com.bd.erecruitment.dto.res.NotificationPollResDTO;
import com.bd.erecruitment.dto.res.NotificationResDTO;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.enums.NotificationType;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class UtcDateTimeTest {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JobCircularRepo jobCircularRepo;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void cleanUp() {
		jobCircularRepo.deleteAll();
	}

	@Test
	void theJvmRunsInUtcSoDateColumnsAreNotShiftedByTheHostZone() {
		assertThat(TimeZone.getDefault().getID()).isEqualTo("UTC");
	}

	@Test
	void dateColumn_keepsTheUtcCalendarDateOfTheInstantSent() {
		Long id = saveJobWithDeadline("2030-01-15T20:00:00Z");

		String stored = jdbcTemplate.queryForObject("SELECT CAST(application_dead_line AS VARCHAR) FROM job_circular WHERE id = ?", String.class, id);
		Date readBack = jobCircularRepo.findById(id).orElseThrow().getApplicationDeadLine();

		assertThat(stored).isEqualTo("2030-01-15");
		assertThat(Instant.ofEpochMilli(readBack.getTime())).isEqualTo(Instant.parse("2030-01-15T00:00:00Z"));
	}

	@Test
	void notificationTimestampsAreSerializedAsUtcWithAZSuffix() throws Exception {
		NotificationResDTO notification = new NotificationResDTO();
		notification.setType(NotificationType.OFFER_RECEIVED);
		notification.setCreatedOn(Date.from(Instant.parse("2030-03-04T05:06:07.089Z")));
		NotificationPollResDTO poll = new NotificationPollResDTO(1, 9L, Date.from(Instant.parse("2030-03-04T05:06:07.000Z")));

		assertThat(field(notification, "createdOn")).isEqualTo("2030-03-04T05:06:07.089Z");
		assertThat(field(poll, "serverTime")).isEqualTo("2030-03-04T05:06:07.000Z");
	}

	@Test
	void notificationTimestampsStayUtcWhateverTheDefaultZoneIs() throws Exception {
		TimeZone original = TimeZone.getDefault();
		try {
			TimeZone.setDefault(TimeZone.getTimeZone("Asia/Dhaka"));
			NotificationResDTO notification = new NotificationResDTO();
			notification.setCreatedOn(Date.from(Instant.parse("2030-03-04T23:30:00Z")));

			assertThat(field(notification, "createdOn")).isEqualTo("2030-03-04T23:30:00.000Z");
		} finally {
			TimeZone.setDefault(original);
		}
	}

	private String field(Object dto, String name) throws Exception {
		return objectMapper.readTree(objectMapper.writeValueAsString(dto)).get(name).asText();
	}

	private Long saveJobWithDeadline(String sentInstant) {
		JobCircular job = new JobCircular().setJobTitle("Deadline check").setStatus("PUBLISHED")
				.setApplicationDeadLine(Date.from(Instant.parse(sentInstant)));
		job.setCreatedBy("system").setCreatedOn(new Date()).setUpdatedBy("system").setUpdatedOn(new Date()).setDeleted(false);
		return jobCircularRepo.save(job).getId();
	}
}
