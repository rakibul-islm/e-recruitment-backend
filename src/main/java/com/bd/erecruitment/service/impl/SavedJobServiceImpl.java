package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.SavedJobResDTO;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.SavedJob;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.SavedJobRepo;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SavedJobServiceImpl {

	private final SavedJobRepo savedJobRepo;
	private final JobCircularRepo jobCircularRepo;

	@Transactional
	public Response<Map<String, Boolean>> toggle(Long jobCircularId) {
		Long userId = currentUserId();
		var existing = savedJobRepo.findByUserIdAndJobCircularIdAndDeleted(userId, jobCircularId, false);

		boolean nowSaved;
		if (existing.isPresent()) {
			savedJobRepo.delete(existing.get());
			nowSaved = false;
		} else {
			String actor = currentUsername();
			Date now = new Date();
			SavedJob savedJob = new SavedJob().setUserId(userId).setJobCircularId(jobCircularId).setSavedOn(now);
			savedJob.setCreatedBy(actor).setCreatedOn(now).setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);
			savedJobRepo.save(savedJob);
			nowSaved = true;
		}

		Response<Map<String, Boolean>> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage(nowSaved ? "Job saved" : "Job removed from saved list");
		response.setObj(Map.of("saved", nowSaved));
		return response;
	}

	public Response<SavedJobResDTO> myList() {
		Long userId = currentUserId();
		List<SavedJobResDTO> list = savedJobRepo.findAllByUserIdAndDeletedOrderBySavedOnDesc(userId, false).stream()
			.map(this::toDto)
			.toList();

		Response<SavedJobResDTO> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage(list.isEmpty() ? "No data found" : "Found");
		response.setList(list);
		return response;
	}

	private SavedJobResDTO toDto(SavedJob savedJob) {
		SavedJobResDTO dto = new SavedJobResDTO(savedJob);
		jobCircularRepo.findByIdAndDeleted(savedJob.getJobCircularId(), false).ifPresent(job -> {
			dto.setJobTitle(job.getJobTitle());
			dto.setCompanyName(job.getCompanyName());
			dto.setJobStatus(job.getStatus());
		});
		return dto;
	}

	private Long currentUserId() {
		var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.bd.erecruitment.model.MyUserDetail mud) {
			return mud.getId();
		}
		throw new com.bd.erecruitment.exception.NotFoundException("Not authenticated");
	}

	private String currentUsername() {
		var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof com.bd.erecruitment.model.MyUserDetail mud) {
			return mud.getUsername();
		}
		return "system";
	}
}
