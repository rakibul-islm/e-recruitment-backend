package com.bd.erecruitment.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asl.issuetrack.dto.req.UserTypeReqDTO;
import com.asl.issuetrack.dto.res.UserTypeResDTO;
import com.asl.issuetrack.entity.UserType;
import com.asl.issuetrack.repository.UserTypeRepo;
import com.asl.issuetrack.service.UserTypeService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;

@Service
public class UserTypeServiceImpl extends AbstractBaseService<UserType, UserTypeResDTO, UserTypeReqDTO> implements UserTypeService<UserTypeResDTO, UserTypeReqDTO> {

	private UserTypeRepo userTypeRepo;

	UserTypeServiceImpl(UserTypeRepo userTypeRepo){
		super(userTypeRepo);
		this.userTypeRepo = userTypeRepo;
	}

	@Override
	public Response<UserTypeResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<UserType> o = userTypeRepo.findById(id);
		return o.isPresent() ? getSuccessResponse("UserType found", new UserTypeResDTO(o.get())) : getErrorResponse("UserType not found");
	}

	@Transactional
	@Override
	public Response<UserTypeResDTO> save(UserTypeReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		UserType z = reqDto.getBean();
	//	z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new UserTypeResDTO(z));
	}

	@Transactional
	@Override
	public Response<UserTypeResDTO> update(UserTypeReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("UserType ID required");
		
		UserType z = reqDto.getBean();
		UserType exist = null;
		Optional<UserType> o = userTypeRepo.findById(z.getId());
		if (!o.isPresent()) return getErrorResponse("Can't found UserType");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new UserTypeResDTO(exist));
	}

	@Transactional
	@Override
	public Response<UserTypeResDTO> delete(UserTypeReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("UserType ID required");

		UserType z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<UserTypeResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("UserType ID required");

		UserType exist = null;
		Optional<UserType> o = userTypeRepo.findById(id);
		if (!o.isPresent()) return getErrorResponse("Can't found UserType");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<UserTypeResDTO> getAll() throws ServiceException{
		List<UserType> list = userTypeRepo.findAll();
		if (list == null || list.isEmpty()) return getErrorResponse("UserType list not found");
		return getSuccessResponse("Found UserType", list.stream().map(data -> new ModelMapper().map(data, UserTypeResDTO.class)).collect(Collectors.toList()));
	}

}
