package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.CandidateProfileReqDto;
import com.bd.erecruitment.dto.res.CandidateProfileResDTO;
import com.bd.erecruitment.dto.res.GeneratedCvResDTO;
import com.bd.erecruitment.entity.CandidateProfile;
import com.bd.erecruitment.entity.GeneratedCv;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.CandidateProfileRepo;
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
public class CandidateProfileServiceImpl {

	private final CandidateProfileRepo candidateProfileRepo;
	private final GeneratedCvRepo generatedCvRepo;
	private final UserRepo userRepo;
	private final CvGenerationService cvGenerationService;
	private final StorageService storageService;

	// Self-proxy so createProfile()'s REQUIRES_NEW applies; a plain this.createProfile() bypasses the transactional proxy.
	@Lazy
	@Autowired
	private CandidateProfileServiceImpl self;

	@Transactional
	public Response<CandidateProfileResDTO> getMyProfile() {
		CandidateProfile profile = getOrCreateProfile(currentUserId());
		return getSuccess("Profile found", new CandidateProfileResDTO(profile));
	}

	@Transactional
	public Response<CandidateProfileResDTO> updateMyProfile(CandidateProfileReqDto reqDto) {
		Long userId = currentUserId();
		CandidateProfile profile = getOrCreateProfile(userId);

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
		return getSuccess("Profile updated successfully", new CandidateProfileResDTO(profile));
	}

	@Transactional
	public Response<GeneratedCvResDTO> generateCv() {
		Long userId = currentUserId();
		CandidateProfile profile = getOrCreateProfile(userId);
		User user = userRepo.findByIdAndDeleted(userId, false)
			.orElseThrow(() -> new NotFoundException("User not found"));

		retirePreviousCvs(profile.getId());
		GeneratedCv cv = cvGenerationService.generate(user, profile);
		return getSuccess("CV generated successfully", new GeneratedCvResDTO(cv));
	}

	private void retirePreviousCvs(Long candidateProfileId) {
		List<GeneratedCv> previous = generatedCvRepo.findAllByCandidateProfileIdAndDeletedOrderByGeneratedOnDesc(candidateProfileId, false);
		if (previous.isEmpty()) return;

		String actor = currentUsername();
		java.util.Date now = new java.util.Date();
		previous.forEach(cv -> cv.setDeleted(true).setUpdatedBy(actor).setUpdatedOn(now));
		generatedCvRepo.saveAll(previous);
	}

	public Response<GeneratedCvResDTO> listMyCvGenerations() {
		CandidateProfile profile = getOrCreateProfile(currentUserId());
		List<GeneratedCvResDTO> list = generatedCvRepo
			.findAllByCandidateProfileIdAndDeletedOrderByGeneratedOnDesc(profile.getId(), false)
			.stream().map(GeneratedCvResDTO::new).toList();
		return getSuccess(list.isEmpty() ? "No data found" : "Found", list);
	}

	public StoredFile downloadMyCv(Long generatedCvId) {
		Long userId = currentUserId();
		CandidateProfile profile = getOrCreateProfile(userId);
		GeneratedCv cv = generatedCvRepo.findByIdAndDeleted(generatedCvId, false)
			.orElseThrow(() -> new NotFoundException("CV not found"));
		if (!cv.getCandidateProfileId().equals(profile.getId())) {
			throw new NotFoundException("CV not found");
		}
		return storageService.retrieve(cv.getStoredFileId());
	}

	private CandidateProfile getOrCreateProfile(Long userId) {
		return candidateProfileRepo.findByUserIdAndDeleted(userId, false).orElseGet(() -> {
			try {
				return self.createProfile(userId);
			// Lost a concurrent insert race on user_id: re-read the winner's row here, outside createProfile's transaction.
			} catch (DataIntegrityViolationException e) {
				return candidateProfileRepo.findByUserIdAndDeleted(userId, false).orElseThrow(() -> e);
			}
		});
	}

	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public CandidateProfile createProfile(Long userId) {
		CandidateProfile profile = new CandidateProfile().setUserId(userId);
		String actor = currentUsername();
		java.util.Date now = new java.util.Date();
		profile.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		return candidateProfileRepo.saveAndFlush(profile);
	}

	private CandidateProfile saveProfile(CandidateProfile profile) {
		String actor = currentUsername();
		java.util.Date now = new java.util.Date();
		if (profile.getId() == null) {
			profile.setCreatedBy(actor).setCreatedOn(now);
		}
		profile.setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
		return candidateProfileRepo.save(profile);
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
