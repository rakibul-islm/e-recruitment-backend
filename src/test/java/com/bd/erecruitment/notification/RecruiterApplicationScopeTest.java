package com.bd.erecruitment.notification;

import com.bd.erecruitment.dto.req.ApplicationStatusChangeReqDto;
import com.bd.erecruitment.dto.res.ApplicationResDTO;
import com.bd.erecruitment.entity.Application;
import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.Permission;
import com.bd.erecruitment.entity.Role;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ApplicationRepo;
import com.bd.erecruitment.repository.ApplicationStatusHistoryRepo;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.NotificationRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.impl.ApplicationServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class RecruiterApplicationScopeTest {

	private static final Long ORGANIZATION_A = 101L;
	private static final Long ORGANIZATION_B = 202L;

	@MockBean
	private MailService mailService;

	@Autowired
	private ApplicationServiceImpl applicationService;

	@Autowired
	private ApplicationRepo applicationRepo;

	@Autowired
	private ApplicationStatusHistoryRepo historyRepo;

	@Autowired
	private JobCircularRepo jobCircularRepo;

	@Autowired
	private NotificationRepo notificationRepo;

	@Autowired
	private UserRepo userRepo;

	private JobCircular jobA;
	private JobCircular jobB;
	private Application applicationA;
	private Application applicationB;
	private Long candidateId;

	@BeforeEach
	void setUp() {
		cleanUp();
		candidateId = userRepo.findByEmail("test@e-recruitment.com").getId();
		jobA = jobCircularRepo.save(stamp(new JobCircular().setJobTitle("Job of A").setStatus("PUBLISHED").setOrganizationId(ORGANIZATION_A)));
		jobB = jobCircularRepo.save(stamp(new JobCircular().setJobTitle("Job of B").setStatus("PUBLISHED").setOrganizationId(ORGANIZATION_B)));
		applicationA = applicationRepo.save(stamp(new Application().setJobCircularId(jobA.getId()).setCandidateUserId(candidateId).setStatus("APPLIED").setAppliedOn(new Date())));
		applicationB = applicationRepo.save(stamp(new Application().setJobCircularId(jobB.getId()).setCandidateUserId(candidateId).setStatus("APPLIED").setAppliedOn(new Date())));
	}

	@AfterEach
	void cleanUp() {
		SecurityContextHolder.clearContext();
		historyRepo.deleteAll();
		applicationRepo.deleteAll();
		jobCircularRepo.deleteAll();
		notificationRepo.deleteAll();
	}

	@Test
	void recruiterSearchShowsOnlyApplicationsForTheirOwnOrganizationsJobs() {
		signInAs(9001L, ORGANIZATION_A, "RECRUITER", "job-circular:write");

		assertThat(searchIds(Map.of())).containsExactly(applicationA.getId());
	}

	@Test
	void recruiterCannotWidenTheSearchToAnotherOrganizationsJobOrPagedResults() {
		signInAs(9001L, ORGANIZATION_A, "RECRUITER", "job-circular:write");

		assertThat(searchIds(Map.of("jobCircularId", String.valueOf(jobB.getId())))).isEmpty();
		assertThat(applicationService.filter(Map.of(), PageRequest.of(0, 10), true).getPage().getContent())
				.extracting(ApplicationResDTO::getId).containsExactly(applicationA.getId());
	}

	@Test
	void recruiterWithoutAnOrganizationSeesNothing() {
		signInAs(9002L, null, "RECRUITER", "job-circular:write");

		assertThat(searchIds(Map.of())).isEmpty();
	}

	@Test
	void recruiterCanOpenAndUpdateTheirOwnApplicationButNotAnotherOrganizations() {
		signInAs(9001L, ORGANIZATION_A, "RECRUITER", "job-circular:write");

		assertThat(applicationService.find(applicationA.getId()).getObj().getId()).isEqualTo(applicationA.getId());
		assertThatThrownBy(() -> applicationService.find(applicationB.getId())).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> applicationService.getHistory(applicationB.getId())).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> applicationService.downloadCv(applicationB.getId())).isInstanceOf(NotFoundException.class);

		ApplicationStatusChangeReqDto change = new ApplicationStatusChangeReqDto();
		change.setStatus("SCREENING");
		assertThatThrownBy(() -> applicationService.changeStatus(applicationB.getId(), change)).isInstanceOf(ForbiddenException.class);
		assertThat(applicationRepo.findById(applicationB.getId()).orElseThrow().getStatus()).isEqualTo("APPLIED");

		applicationService.changeStatus(applicationA.getId(), change);
		assertThat(applicationRepo.findById(applicationA.getId()).orElseThrow().getStatus()).isEqualTo("SCREENING");
	}

	@Test
	void administratorsAndUnrestrictedRolesStillSeeEveryOrganization() {
		signInAs(9003L, null, "ADMINISTRATOR", "SUPER_ADMIN");
		assertThat(searchIds(Map.of())).containsExactlyInAnyOrder(applicationA.getId(), applicationB.getId());

		signInAs(9004L, ORGANIZATION_A, "MANAGER", "job-circular:write");
		assertThat(searchIds(Map.of())).containsExactlyInAnyOrder(applicationA.getId(), applicationB.getId());
		assertThat(applicationService.find(applicationB.getId()).getObj().getId()).isEqualTo(applicationB.getId());
	}

	@Test
	void aCandidateStillSeesTheirOwnApplicationButNotSomeoneElses() {
		signInAs(candidateId, null, "REGISTERED_USER");
		assertThat(applicationService.find(applicationA.getId()).getObj().getId()).isEqualTo(applicationA.getId());

		signInAs(9005L, null, "REGISTERED_USER");
		assertThatThrownBy(() -> applicationService.find(applicationA.getId())).isInstanceOf(ForbiddenException.class);
	}

	private List<Long> searchIds(Map<String, String> filters) {
		return applicationService.filter(filters, null, false).getList().stream().map(ApplicationResDTO::getId).toList();
	}

	private void signInAs(Long userId, Long organizationId, String roleCode, String... authorities) {
		Role role = new Role();
		role.setCode(roleCode);
		role.setPermissions(new HashSet<>());
		for (String authority : authorities) {
			Permission permission = new Permission();
			permission.setAuthority(authority);
			role.getPermissions().add(permission);
		}
		Calendar farFuture = Calendar.getInstance();
		farFuture.add(Calendar.YEAR, 10);
		User user = User.builder().id(userId).email("scope-test-" + userId + "@example.com").active(true)
				.expiryDate(farFuture.getTime()).organizationId(organizationId).roles(Set.of(role)).build();
		MyUserDetail principal = new MyUserDetail(user);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
	}

	private <T extends BaseEntity> T stamp(T entity) {
		Date now = new Date();
		entity.setCreatedBy("system").setCreatedOn(now).setUpdatedBy("system").setUpdatedOn(now).setDeleted(false);
		return entity;
	}
}
