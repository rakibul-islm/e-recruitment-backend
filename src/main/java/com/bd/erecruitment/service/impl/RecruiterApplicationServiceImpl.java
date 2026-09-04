package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.RecruiterApplicationRejectReqDto;
import com.bd.erecruitment.dto.req.RecruiterApplicationReqDto;
import com.bd.erecruitment.dto.req.UserReqDto;
import com.bd.erecruitment.dto.res.RecruiterApplicationResDTO;
import com.bd.erecruitment.dto.res.UserResDTO;
import com.bd.erecruitment.entity.Company;
import com.bd.erecruitment.entity.RecruiterApplication;
import com.bd.erecruitment.entity.Role;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.CompanyRepo;
import com.bd.erecruitment.repository.RecruiterApplicationRepo;
import com.bd.erecruitment.repository.RoleRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.MailService;
import com.bd.erecruitment.service.UserService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class RecruiterApplicationServiceImpl extends AbstractBaseService<RecruiterApplication>
		implements BaseService<RecruiterApplicationResDTO, RecruiterApplicationReqDto> {

	private static final String READ_AUTHORITY = "recruiter-application:read";
	private static final String WRITE_AUTHORITY = "recruiter-application:write";

	private final RecruiterApplicationRepo recruiterApplicationRepo;
	private final UserRepo userRepo;
	private final RoleRepo roleRepo;
	private final CompanyRepo companyRepo;
	private final UserService<UserResDTO, UserReqDto> userService;
	private final MailService mailService;

	public RecruiterApplicationServiceImpl(RecruiterApplicationRepo recruiterApplicationRepo, UserRepo userRepo,
			RoleRepo roleRepo, CompanyRepo companyRepo, UserService<UserResDTO, UserReqDto> userService, MailService mailService) {
		super(recruiterApplicationRepo);
		this.recruiterApplicationRepo = recruiterApplicationRepo;
		this.userRepo = userRepo;
		this.roleRepo = roleRepo;
		this.companyRepo = companyRepo;
		this.userService = userService;
		this.mailService = mailService;
	}

	@Override
	public Response<RecruiterApplicationResDTO> find(Long id) {
		requirePermission(READ_AUTHORITY);
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Application found", new RecruiterApplicationResDTO(findByIdOrThrow(id, "Application not found")));
	}

	// The public submission entry point (POST /recruiter-application, exposed by AbstractBaseController.save()).
	@Transactional
	@Override
	public Response<RecruiterApplicationResDTO> save(RecruiterApplicationReqDto reqDto) {
		validateForm(reqDto);
		if (userRepo.findByEmail(reqDto.getEmail()) != null) returnErrorException("An account with this email already exists");
		recruiterApplicationRepo.findFirstByEmailAndStatusAndDeleted(reqDto.getEmail(), "PENDING", false)
			.ifPresent(existing -> returnErrorException("A request for this email is already pending review"));

		// Anonymous submission: no logged-in actor to attribute createdBy/On to, same as candidate self-signup.
		RecruiterApplication application = createNormalUser(reqDto.getBean());
		sendReceivedEmail(application);
		return getCreatedResponse("Application submitted. We'll review it and get back to you.", new RecruiterApplicationResDTO(application));
	}

	@Override
	public Response<RecruiterApplicationResDTO> update(RecruiterApplicationReqDto reqDto) {
		returnErrorException("Use the approve/reject actions instead of a direct update");
		return null;
	}

	@Transactional
	@Override
	public Response<RecruiterApplicationResDTO> delete(Long id) {
		requirePermission(WRITE_AUTHORITY);
		deleteEntity(findByIdOrThrow(id, "Application not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<RecruiterApplicationResDTO> remove(Long id) {
		requirePermission(WRITE_AUTHORITY);
		removeEntity(findByIdOrThrow(id, "Application not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<RecruiterApplicationResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		requirePermission(READ_AUTHORITY);
		return genericFilter(filters, pageable, isPageable, RecruiterApplicationResDTO.class);
	}

	@Transactional
	public Response<RecruiterApplicationResDTO> approve(Long id) {
		requirePermission(WRITE_AUTHORITY);
		RecruiterApplication application = findByIdOrThrow(id, "Application not found");
		if (!"PENDING".equals(application.getStatus())) returnErrorException("Only pending applications can be approved");

		Role recruiterRole = roleRepo.findByCode("RECRUITER");
		if (recruiterRole == null) returnErrorException("Recruiter role is not configured");

		Company company = resolveOrCreateCompany(application);

		// Reuses the same admin-invite path /users/create already goes through: inactive account,
		// activation token, "set your password" email - no duplicated logic here.
		UserReqDto userReqDto = new UserReqDto();
		userReqDto.setFullName(application.getFullName());
		userReqDto.setEmail(application.getEmail());
		userReqDto.setMobile(application.getPhone());
		userReqDto.setRoleIds(Set.of(recruiterRole.getId()));
		userReqDto.setCompanyId(company.getId());
		userService.save(userReqDto);

		application.setStatus("APPROVED");
		application = updateEntity(application);
		return getSuccessResponse("Application approved. The recruiter has been emailed to set up their account.", new RecruiterApplicationResDTO(application));
	}

	@Transactional
	public Response<RecruiterApplicationResDTO> reject(Long id, RecruiterApplicationRejectReqDto reqDto) {
		requirePermission(WRITE_AUTHORITY);
		RecruiterApplication application = findByIdOrThrow(id, "Application not found");
		if (!"PENDING".equals(application.getStatus())) returnErrorException("Only pending applications can be rejected");

		application.setStatus("REJECTED");
		application.setReviewNote(reqDto.getNote());
		application = updateEntity(application);
		sendRejectedEmail(application);
		return getSuccessResponse("Application rejected", new RecruiterApplicationResDTO(application));
	}

	// The recruiter's company needs to exist (and be linked via User.companyId) for them to view/
	// manage once approved - reuse a matching-by-name company if one already exists, otherwise
	// create it from the details they gave at registration. Saved directly via the repo (rather
	// than createEntity(), which AbstractBaseService types to this class's own entity,
	// RecruiterApplication, not Company).
	private Company resolveOrCreateCompany(RecruiterApplication application) {
		return companyRepo.findFirstByNameIgnoreCaseAndDeleted(application.getCompanyName(), false)
			.orElseGet(() -> {
				String actor = getLoggedInUserDetails().getUsername();
				java.util.Date now = new java.util.Date();
				Company company = new Company()
					.setName(application.getCompanyName())
					.setWebsite(application.getCompanyWebsite())
					.setIndustry(application.getCompanyIndustry())
					.setSize(application.getCompanySize())
					.setAddress(application.getCompanyAddress())
					.setPhone(application.getCompanyPhone())
					.setEmail(application.getCompanyEmail())
					.setDescription(application.getCompanyDescription());
				company.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
				return companyRepo.save(company);
			});
	}

	private void sendReceivedEmail(RecruiterApplication application) {
		try {
			mailService.sendRecruiterApplicationReceivedEmail(application.getEmail(), application.getFullName(), application.getCompanyName());
		} catch (Exception ignored) {
			// Never fail the submission because of an email delivery problem.
		}
	}

	private void sendRejectedEmail(RecruiterApplication application) {
		try {
			mailService.sendRecruiterApplicationRejectedEmail(application.getEmail(), application.getFullName(), application.getReviewNote());
		} catch (Exception ignored) {
			// Never fail the rejection because of an email delivery problem.
		}
	}

	private void validateForm(RecruiterApplicationReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getFullName())) returnErrorException("Full name required");
		if (StringUtils.isBlank(reqDto.getEmail())) returnErrorException("Work email required");
		if (StringUtils.isBlank(reqDto.getCompanyName())) returnErrorException("Company name required");
	}

	private void requirePermission(String authority) {
		MyUserDetail me = getLoggedInUserDetails();
		if (me == null) throw new ForbiddenException("Access denied");
		boolean allowed = me.getAuthorities().stream().anyMatch(a ->
			authority.equals(a.getAuthority()) || "SUPER_ADMIN".equals(a.getAuthority()));
		if (!allowed) throw new ForbiddenException("You do not have permission to perform this action");
	}
}
