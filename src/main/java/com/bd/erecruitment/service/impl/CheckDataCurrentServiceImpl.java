package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.CheckDataCurrentReqDTO;
import com.asl.issuetrack.dto.res.CheckDataCurrentResDTO;
import com.asl.issuetrack.entity.CheckDataCurrent;
import com.asl.issuetrack.repository.CheckDataCurrentRepo;
import com.asl.issuetrack.service.CheckDataCurrentService;
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
public class CheckDataCurrentServiceImpl extends AbstractBaseService<CheckDataCurrent, CheckDataCurrentResDTO, CheckDataCurrentReqDTO> implements CheckDataCurrentService<CheckDataCurrentResDTO, CheckDataCurrentReqDTO> {

	private CheckDataCurrentRepo checkDataCurrentRepo;

	CheckDataCurrentServiceImpl(CheckDataCurrentRepo checkDataCurrentRepo){
		super(checkDataCurrentRepo);
		this.checkDataCurrentRepo = checkDataCurrentRepo;
	}

	@Override
	public Response<CheckDataCurrentResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<CheckDataCurrent> o = checkDataCurrentRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("CheckDataCurrent found", new CheckDataCurrentResDTO(o.get())) : getErrorResponse("CheckDataCurrent not found");
	}

	@Transactional
	@Override
	public Response<CheckDataCurrentResDTO> save(CheckDataCurrentReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		CheckDataCurrent z = reqDto.getBean();
		z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new CheckDataCurrentResDTO(z));
	}

	@Transactional
	@Override
	public Response<CheckDataCurrentResDTO> update(CheckDataCurrentReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("CheckDataCurrent ID required");
		
		CheckDataCurrent z = reqDto.getBean();
		CheckDataCurrent exist = null;
		Optional<CheckDataCurrent> o = checkDataCurrentRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found CheckDataCurrent");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new CheckDataCurrentResDTO(exist));
	}

	@Transactional
	@Override
	public Response<CheckDataCurrentResDTO> delete(CheckDataCurrentReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("CheckDataCurrent ID required");

		CheckDataCurrent z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<CheckDataCurrentResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("CheckDataCurrent ID required");

		CheckDataCurrent exist = null;
		Optional<CheckDataCurrent> o = checkDataCurrentRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found CheckDataCurrent");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<CheckDataCurrentResDTO> getAll() throws ServiceException{
		List<CheckDataCurrent> list = checkDataCurrentRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("CheckDataCurrent list not found");
		return getSuccessResponse("Found CheckDataCurrent", list.stream().map(data -> new ModelMapper().map(data, CheckDataCurrentResDTO.class)).collect(Collectors.toList()));
	}

}
