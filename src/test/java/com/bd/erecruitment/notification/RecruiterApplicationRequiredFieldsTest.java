package com.bd.erecruitment.notification;

import com.bd.erecruitment.dto.req.RecruiterApplicationReqDto;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.repository.RecruiterApplicationRepo;
import com.bd.erecruitment.service.impl.RecruiterApplicationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RecruiterApplicationRequiredFieldsTest {

	@Autowired
	private RecruiterApplicationServiceImpl service;

	@Autowired
	private RecruiterApplicationRepo repo;

	@AfterEach
	void cleanUp() {
		repo.deleteAll();
	}

	@Test
	void everyImportantFieldIsRequired() {
		assertRejected(f -> f.setFullName(" "), "Full name required");
		assertRejected(f -> f.setEmail(null), "Work email required");
		assertRejected(f -> f.setPhone(""), "Mobile required");
		assertRejected(f -> f.setOrganizationName(null), "Organization name required");
		assertRejected(f -> f.setOrganizationSector(null), "Organization sector required");
		assertRejected(f -> f.setOrganizationAddress(" "), "Organization address required");
		assertRejected(f -> f.setOrganizationPhone(""), "Organization phone required");
		assertRejected(f -> f.setOrganizationEmail(null), "Organization email required");
		assertRejected(f -> f.setJobTitle(" "), "Job title required");
	}

	@Test
	void websiteSizeAndMessageStayOptional() {
		RecruiterApplicationReqDto form = validForm();
		form.setOrganizationWebsite(null);
		form.setOrganizationSize(null);
		form.setMessage(null);

		service.save(form);

		org.assertj.core.api.Assertions.assertThat(repo.count()).isEqualTo(1);
	}

	private void assertRejected(Consumer<RecruiterApplicationReqDto> breakIt, String message) {
		RecruiterApplicationReqDto form = validForm();
		breakIt.accept(form);

		assertThatThrownBy(() -> service.save(form)).isInstanceOf(BadRequestException.class).hasMessageContaining(message);
	}

	private RecruiterApplicationReqDto validForm() {
		RecruiterApplicationReqDto form = new RecruiterApplicationReqDto();
		form.setFullName("Nadia Rahman");
		form.setEmail("required.fields@example.com");
		form.setPhone("01700000000");
		form.setOrganizationName("Acme Ltd");
		form.setOrganizationSector("Private Limited Company");
		form.setOrganizationAddress("Dhaka");
		form.setOrganizationPhone("029999999");
		form.setOrganizationEmail("hr@acme.example.com");
		form.setJobTitle("HR Manager");
		form.setOrganizationWebsite("https://acme.example.com");
		form.setOrganizationSize("50");
		form.setMessage("Hiring");
		return form;
	}
}
