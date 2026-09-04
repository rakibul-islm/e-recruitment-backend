package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.JobAlertReqDto;
import com.bd.erecruitment.dto.res.JobAlertResDTO;
import com.bd.erecruitment.entity.JobAlert;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.JobAlertRepo;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobAlertServiceImpl {

	private final JobAlertRepo jobAlertRepo;

	@Transactional
	public Response<JobAlertResDTO> save(JobAlertReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getKeyword()) && StringUtils.isBlank(reqDto.getLocation()) && StringUtils.isBlank(reqDto.getCategory())) {
			Response<JobAlertResDTO> error = new Response<>();
			error.setCode(400);
			error.setSuccess(false);
			error.setMessage("At least one of keyword, location or category is required");
			return error;
		}

		Long userId = currentUserId();
		String actor = currentUsername();
		Date now = new Date();

		JobAlert alert;
		if (reqDto.getId() != null) {
			alert = jobAlertRepo.findByIdAndDeleted(reqDto.getId(), false)
				.orElseThrow(() -> new NotFoundException("Job alert not found"));
			if (!alert.getUserId().equals(userId)) throw new ForbiddenException("Access denied");
		} else {
			alert = new JobAlert().setUserId(userId);
			alert.setCreatedBy(actor).setCreatedOn(now);
		}

		alert.setKeyword(reqDto.getKeyword())
			.setLocation(reqDto.getLocation())
			.setCategory(reqDto.getCategory())
			.setActive(reqDto.getActive() == null || reqDto.getActive());
		alert.setUpdatedBy(actor).setUpdatedOn(now).setDeleted(false);

		JobAlert saved = jobAlertRepo.save(alert);
		return success(new JobAlertResDTO(saved));
	}

	public Response<JobAlertResDTO> myList() {
		Long userId = currentUserId();
		List<JobAlertResDTO> list = jobAlertRepo.findAllByUserIdAndDeletedOrderByIdDesc(userId, false)
			.stream().map(JobAlertResDTO::new).toList();
		Response<JobAlertResDTO> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage(list.isEmpty() ? "No data found" : "Found");
		response.setList(list);
		return response;
	}

	@Transactional
	public Response<JobAlertResDTO> remove(Long id) {
		Long userId = currentUserId();
		JobAlert alert = jobAlertRepo.findByIdAndDeleted(id, false).orElseThrow(() -> new NotFoundException("Job alert not found"));
		if (!alert.getUserId().equals(userId)) throw new ForbiddenException("Access denied");

		alert.setDeleted(true).setUpdatedBy(currentUsername()).setUpdatedOn(new Date());
		jobAlertRepo.save(alert);

		Response<JobAlertResDTO> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage("Removed successfully");
		return response;
	}

	private Response<JobAlertResDTO> success(JobAlertResDTO obj) {
		Response<JobAlertResDTO> response = new Response<>();
		response.setCode(200);
		response.setSuccess(true);
		response.setMessage("Saved successfully");
		response.setObj(obj);
		return response;
	}

	private Long currentUserId() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof MyUserDetail mud) {
			return mud.getId();
		}
		throw new NotFoundException("Not authenticated");
	}

	private String currentUsername() {
		var auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof MyUserDetail mud) {
			return mud.getUsername();
		}
		return "system";
	}
}
