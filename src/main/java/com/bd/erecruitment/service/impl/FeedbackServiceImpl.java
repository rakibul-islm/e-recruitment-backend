package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.FeedbackReqDTO;
import com.asl.issuetrack.dto.res.FeedbackResDTO;
import com.asl.issuetrack.entity.Feedback;
import com.asl.issuetrack.entity.User;
import com.asl.issuetrack.enums.UserRole;
import com.asl.issuetrack.repository.FeedbackRepo;
import com.asl.issuetrack.service.FeedbackService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FeedbackServiceImpl extends AbstractBaseService<Feedback, FeedbackResDTO, FeedbackReqDTO> implements FeedbackService<FeedbackResDTO, FeedbackReqDTO> {

	private FeedbackRepo feedbackRepo;

	FeedbackServiceImpl(FeedbackRepo feedbackRepo){
		super(feedbackRepo);
		this.feedbackRepo = feedbackRepo;
	}

	@Override
	public Response<FeedbackResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<Feedback> o = feedbackRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("Feedback found", new FeedbackResDTO(o.get())) : getErrorResponse("Feedback not found");
	}

	@Transactional
	@Override
	public Response<FeedbackResDTO> save(FeedbackReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		Feedback z = reqDto.getBean();
		z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new FeedbackResDTO(z));
	}

	@Transactional
	@Override
	public Response<FeedbackResDTO> update(FeedbackReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Feedback ID required");
		
		Feedback z = reqDto.getBean();
		Feedback exist = null;
		Optional<Feedback> o = feedbackRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found Feedback");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new FeedbackResDTO(exist));
	}

	@Transactional
	@Override
	public Response<FeedbackResDTO> delete(FeedbackReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Feedback ID required");

		Feedback z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<FeedbackResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("Feedback ID required");

		Feedback exist = null;
		Optional<Feedback> o = feedbackRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found Feedback");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<FeedbackResDTO> getAll() throws ServiceException{
		List<Feedback> list;
		User user = getLoggedInUser();
		
		if(user.getRoles().equalsIgnoreCase(UserRole.ROLE_SYSTEM_ADMIN.name()) || user.getRoles().equalsIgnoreCase(UserRole.ROLE_SUPER_ADMIN.name())) {
			list = feedbackRepo.findAllByDeleted(false, Sort.by(Sort.Direction.DESC, "id"));
		}else {
			list = feedbackRepo.findAllByUserIdAndDeleted(user.getId(), false, Sort.by(Sort.Direction.DESC, "id"));
		}
		
		if (list == null || list.isEmpty()) return getErrorResponse("Feedback list not found");
		return getSuccessResponse("Found Feedback", list.stream().map(data -> new ModelMapper().map(data, FeedbackResDTO.class)).collect(Collectors.toList()));
	}

}
