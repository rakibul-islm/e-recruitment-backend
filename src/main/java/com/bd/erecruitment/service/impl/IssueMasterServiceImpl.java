package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.IssueCategoryReqDTO;
import com.asl.issuetrack.dto.res.IssueCategoryResDTO;
import com.asl.issuetrack.entity.IssueCategory;
import com.asl.issuetrack.repository.IssueMasterRepo;
import com.asl.issuetrack.service.IssueMasterService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IssueMasterServiceImpl extends AbstractBaseService<IssueCategory, IssueCategoryResDTO, IssueCategoryReqDTO> implements IssueMasterService<IssueCategoryResDTO, IssueCategoryReqDTO> {

	private IssueMasterRepo issueMasterRepo;

	IssueMasterServiceImpl(IssueMasterRepo issueMasterRepo){
		super(issueMasterRepo);
		this.issueMasterRepo = issueMasterRepo;
	}

	@Override
	public Response<IssueCategoryResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<IssueCategory> o = issueMasterRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("Issue Category found", new IssueCategoryResDTO(o.get())) : getErrorResponse("Issue Category not found");
	}

	@Transactional
	@Override
	public Response<IssueCategoryResDTO> save(IssueCategoryReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		IssueCategory z = reqDto.getBean();
	//	z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new IssueCategoryResDTO(z));
	}

	@Transactional
	@Override
	public Response<IssueCategoryResDTO> update(IssueCategoryReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Issue Category ID required");
		
		IssueCategory z = reqDto.getBean();
		IssueCategory exist = null;
		Optional<IssueCategory> o = issueMasterRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found Issue Category");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new IssueCategoryResDTO(exist));
	}

	@Transactional
	@Override
	public Response<IssueCategoryResDTO> delete(IssueCategoryReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Issue Category ID required");

		IssueCategory z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<IssueCategoryResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("Issue Category ID required");

		IssueCategory exist = null;
		Optional<IssueCategory> o = issueMasterRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found Issue Category");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<IssueCategoryResDTO> getAll() throws ServiceException{
		List<IssueCategory> list = issueMasterRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("Issue Category list not found");
		return getSuccessResponse("Found Issue Category", list.stream().map(data -> new ModelMapper().map(data, IssueCategoryResDTO.class)).collect(Collectors.toList()));
	}

}
