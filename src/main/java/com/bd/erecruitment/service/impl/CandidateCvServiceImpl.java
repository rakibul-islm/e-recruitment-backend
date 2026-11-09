package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.audit.AuditAction;
import com.bd.erecruitment.audit.AuditLogWriter;
import com.bd.erecruitment.dto.req.CandidateCvReqDto;
import com.bd.erecruitment.dto.res.CandidateCvResDTO;
import com.bd.erecruitment.dto.res.GeneratedCvResDTO;
import com.bd.erecruitment.entity.CandidateCv;
import com.bd.erecruitment.entity.GeneratedCv;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.CandidateCvRepo;
import com.bd.erecruitment.repository.GeneratedCvRepo;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.CvGenerationService;
import com.bd.erecruitment.service.StorageService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CandidateCvServiceImpl {

	private final CandidateCvRepo candidateCvRepo;
	private final GeneratedCvRepo generatedCvRepo;
	private final UserRepo userRepo;
	private final CvGenerationService cvGenerationService;
	private final StorageService storageService;
	private final AuditLogWriter auditLogWriter;

	// Self-proxy so createProfile()'s REQUIRES_NEW applies; a plain this.createProfile() bypasses the transactional proxy.
	@Lazy
	@Autowired
	private CandidateCvServiceImpl self;

	@Transactional
	public Response<CandidateCvResDTO> getMyProfile() {
		CandidateCv profile = getOrCreateProfile(currentUserId());
		return getSuccess("Profile found", toDto(profile));
	}

	@Transactional
	public Response<CandidateCvResDTO> updateMyProfile(CandidateCvReqDto reqDto) {
		Long userId = currentUserId();
		CandidateCv profile = getOrCreateProfile(userId);

		profile.setHeadline(reqDto.getHeadline())
			.setSummary(reqDto.getSummary())
			.setPhone(reqDto.getPhone())
			.setAddress(reqDto.getAddress())
			.setLinkedinUrl(reqDto.getLinkedinUrl())
			.setPortfolioUrl(reqDto.getPortfolioUrl());
		profile.getWorkExperience().clear();
		if (reqDto.getWorkExperience() != null) profile.getWorkExperience().addAll(reqDto.getWorkExperience());
		profile.getEducation().clear();
		if (reqDto.getEducation() != null) profile.getEducation().addAll(reqDto.getEducation());
		profile.getSkills().clear();
		if (reqDto.getSkills() != null) profile.getSkills().addAll(reqDto.getSkills());
		profile.getCertifications().clear();
		if (reqDto.getCertifications() != null) profile.getCertifications().addAll(reqDto.getCertifications());
		profile.getLanguages().clear();
		if (reqDto.getLanguages() != null) profile.getLanguages().addAll(reqDto.getLanguages());
		profile.getProjects().clear();
		if (reqDto.getProjects() != null) profile.getProjects().addAll(reqDto.getProjects());

		profile = saveProfile(profile);
		auditLogWriter.logEntity(AuditAction.UPDATE, "CandidateCv", profile.getId(), AuditOutcome.SUCCESS, null);
		return getSuccess("Profile updated successfully", toDto(profile));
	}

	@Transactional
	public Response<GeneratedCvResDTO> generateCv() {
		Long userId = currentUserId();
		CandidateCv profile = getOrCreateProfile(userId);
		User user = userRepo.findByIdAndDeleted(userId, false)
			.orElseThrow(() -> new NotFoundException("User not found"));

		GeneratedCv cv = cvGenerationService.generate(user, profile);
		auditLogWriter.logEntity(AuditAction.CREATE, "GeneratedCv", cv.getId(), AuditOutcome.SUCCESS, null);
		return getSuccess("CV generated successfully", new GeneratedCvResDTO(cv));
	}

	public Response<GeneratedCvResDTO> listMyCvGenerations() {
		CandidateCv profile = getOrCreateProfile(currentUserId());
		List<GeneratedCvResDTO> list = generatedCvRepo
			.findAllByCandidateCvIdAndDeletedOrderByGeneratedOnDesc(profile.getId(), false)
			.stream().map(GeneratedCvResDTO::new).toList();
		return getSuccess(list.isEmpty() ? "No data found" : "Found", list);
	}

	public StoredFile downloadMyCv(Long generatedCvId) {
		Long userId = currentUserId();
		CandidateCv profile = getOrCreateProfile(userId);
		GeneratedCv cv = generatedCvRepo.findByIdAndDeleted(generatedCvId, false)
			.orElseThrow(() -> new NotFoundException("CV not found"));
		if (!cv.getCandidateCvId().equals(profile.getId())) {
			throw new NotFoundException("CV not found");
		}
		auditLogWriter.logActivity(AuditAction.DOWNLOAD, "GeneratedCv", cv.getId(), null);
		return storageService.retrieve(cv.getStoredFileId());
	}

	private CandidateCvResDTO toDto(CandidateCv profile) {
		CandidateCvResDTO dto = new CandidateCvResDTO(profile);
		dto.setCompleteness(CvCompletenessCalculator.calculate(profile, userRepo.findByIdAndDeleted(profile.getUserId(), false).orElse(null)));
		return dto;
	}

	private CandidateCv getOrCreateProfile(Long userId) {
		return candidateCvRepo.findByUserIdAndDeleted(userId, false).orElseGet(() -> {
			try {
				return self.createProfile(userId);
			// Lost a concurrent insert race on user_id: re-read the winner's row here, outside createProfile's transaction.
			} catch (DataIntegrityViolationException e) {
				return candidateCvRepo.findByUserIdAndDeleted(userId, false).orElseThrow(() -> e);
			}
		});
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public CandidateCv createProfile(Long userId) {
		CandidateCv profile = new CandidateCv().setUserId(userId);
		String actor = currentUsername();
		java.util.Date now = new java.util.Date();
		profile.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		return candidateCvRepo.saveAndFlush(profile);
	}

	private CandidateCv saveProfile(CandidateCv profile) {
		String actor = currentUsername();
		java.util.Date now = new java.util.Date();
		if (profile.getId() == null) {
			profile.setCreatedBy(actor).setCreatedOn(now);
		}
		profile.setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		return candidateCvRepo.save(profile);
	}

	private Long currentUserId() {
		MyUserDetail user = currentUser();
		return user.getId();
	}

	private String currentUsername() {
		return currentUser().getUsername();
	}

	private MyUserDetail currentUser() {
		var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof MyUserDetail mud)) {
			throw new NotFoundException("Not authenticated");
		}
		return mud;
	}

	private <R> Response<R> getSuccess(String message, R obj) {
		Response<R> res = new Response<>();
		res.setCode(200);
		res.setSuccess(true);
		res.setMessage(message);
		res.setObj(obj);
		return res;
	}

	private <R> Response<R> getSuccess(String message, List<R> list) {
		Response<R> res = new Response<>();
		res.setCode(200);
		res.setSuccess(true);
		res.setMessage(message);
		res.setList(list);
		return res;
	}
}
