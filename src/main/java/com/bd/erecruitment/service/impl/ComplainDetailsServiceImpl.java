package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.ComplainDetailsReqDTO;
import com.asl.issuetrack.dto.res.ComplainDetailsResDTO;
import com.asl.issuetrack.entity.ComplainDetails;
import com.asl.issuetrack.repository.ComplainDetailsRepo;
import com.asl.issuetrack.service.ComplainDetailsService;
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
public class ComplainDetailsServiceImpl extends AbstractBaseService<ComplainDetails, ComplainDetailsResDTO, ComplainDetailsReqDTO> implements ComplainDetailsService<ComplainDetailsResDTO, ComplainDetailsReqDTO> {

	private ComplainDetailsRepo complainDetailsRepo;

	ComplainDetailsServiceImpl(ComplainDetailsRepo complainDetailsRepo){
		super(complainDetailsRepo);
		this.complainDetailsRepo = complainDetailsRepo;
	}

	@Override
	public Response<ComplainDetailsResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<ComplainDetails> o = complainDetailsRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("ComplainDetails found", new ComplainDetailsResDTO(o.get())) : getErrorResponse("ComplainDetails not found");
	}

	@Transactional
	@Override
	public Response<ComplainDetailsResDTO> save(ComplainDetailsReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		ComplainDetails z = reqDto.getBean();
	//	z.setUser(getLoggedInUser());
		z.setComplainMaster(reqDto.getComplainMasterReqDTO().getBean());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new ComplainDetailsResDTO(z));
	}

	@Transactional
	@Override
	public Response<ComplainDetailsResDTO> update(ComplainDetailsReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("ComplainDetails ID required");
		
		ComplainDetails z = reqDto.getBean();
		ComplainDetails exist = null;
		Optional<ComplainDetails> o = complainDetailsRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found ComplainDetails");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new ComplainDetailsResDTO(exist));
	}

	@Transactional
	@Override
	public Response<ComplainDetailsResDTO> delete(ComplainDetailsReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("ComplainDetails ID required");

		ComplainDetails z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<ComplainDetailsResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("ComplainDetails ID required");

		ComplainDetails exist = null;
		Optional<ComplainDetails> o = complainDetailsRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found ComplainDetails");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<ComplainDetailsResDTO> getAll() throws ServiceException{
		List<ComplainDetails> list = complainDetailsRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("ComplainDetails list not found");
		return getSuccessResponse("Found ComplainDetails", list.stream().map(data -> new ModelMapper().map(data, ComplainDetailsResDTO.class)).collect(Collectors.toList()));
	}

}
