package com.bd.erecruitment.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asl.issuetrack.dto.req.ComplainMasterReqDTO;
import com.asl.issuetrack.dto.res.ComplainMasterResDTO;
import com.asl.issuetrack.entity.ComplainMaster;
import com.asl.issuetrack.entity.User;
import com.asl.issuetrack.enums.UserRole;
import com.asl.issuetrack.repository.ComplainMasterRepo;
import com.asl.issuetrack.service.ComplainMasterService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;

@Service
public class ComplainMasterServiceImpl extends AbstractBaseService<ComplainMaster, ComplainMasterResDTO, ComplainMasterReqDTO> implements ComplainMasterService<ComplainMasterResDTO, ComplainMasterReqDTO> {

	private ComplainMasterRepo complainMasterRepo;

	ComplainMasterServiceImpl(ComplainMasterRepo complainMasterRepo){
		super(complainMasterRepo);
		this.complainMasterRepo = complainMasterRepo;
	}

	@Override
	public Response<ComplainMasterResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<ComplainMaster> o = complainMasterRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("ComplainMaster found", new ComplainMasterResDTO(o.get())) : getErrorResponse("ComplainMaster not found");
	}

	@Transactional
	@Override
	public Response<ComplainMasterResDTO> save(ComplainMasterReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		ComplainMaster z = reqDto.getBean();
		z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new ComplainMasterResDTO(z));
	}

	@Transactional
	@Override
	public Response<ComplainMasterResDTO> update(ComplainMasterReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("ComplainMaster ID required");
		
		ComplainMaster z = reqDto.getBean();
		ComplainMaster exist = null;
		Optional<ComplainMaster> o = complainMasterRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found ComplainMaster");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new ComplainMasterResDTO(exist));
	}

	@Transactional
	@Override
	public Response<ComplainMasterResDTO> delete(ComplainMasterReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("ComplainMaster ID required");

		ComplainMaster z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<ComplainMasterResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("ComplainMaster ID required");

		ComplainMaster exist = null;
		Optional<ComplainMaster> o = complainMasterRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found ComplainMaster");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<ComplainMasterResDTO> getAll() throws ServiceException{
		List<ComplainMaster> list;
		User user = getLoggedInUser();
		
		if(user.getRoles().equalsIgnoreCase(UserRole.ROLE_SYSTEM_ADMIN.name()) || user.getRoles().equalsIgnoreCase(UserRole.ROLE_SUPER_ADMIN.name())) {
			list = complainMasterRepo.findAllByDeleted(false, Sort.by(Sort.Direction.DESC, "id"));
		}else {
			list = complainMasterRepo.findAllByUserIdAndDeleted(user.getId(), false, Sort.by(Sort.Direction.DESC, "id"));
		}
		
		if (list == null || list.isEmpty()) return getErrorResponse("ComplainMaster list not found");
		return getSuccessResponse("Found ComplainMaster", list.stream().map(data -> new ModelMapper().map(data, ComplainMasterResDTO.class)).collect(Collectors.toList()));
	}

}
