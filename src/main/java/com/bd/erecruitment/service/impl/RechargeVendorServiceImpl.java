package com.bd.erecruitment.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asl.issuetrack.dto.req.RechargeVendorReqDTO;
import com.asl.issuetrack.dto.res.RechargeVendorResDTO;
import com.asl.issuetrack.entity.RechargeVendor;
import com.asl.issuetrack.repository.RechargeVendorRepo;
import com.asl.issuetrack.service.RechargeVendorService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;

@Service
public class RechargeVendorServiceImpl extends AbstractBaseService<RechargeVendor, RechargeVendorResDTO, RechargeVendorReqDTO> implements RechargeVendorService<RechargeVendorResDTO, RechargeVendorReqDTO> {

	private RechargeVendorRepo rechargeVendorRepo;

	RechargeVendorServiceImpl(RechargeVendorRepo rechargeVendorRepo){
		super(rechargeVendorRepo);
		this.rechargeVendorRepo = rechargeVendorRepo;
	}

	@Override
	public Response<RechargeVendorResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<RechargeVendor> o = rechargeVendorRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("RechargeVendor found", new RechargeVendorResDTO(o.get())) : getErrorResponse("RechargeVendor not found");
	}

	@Transactional
	@Override
	public Response<RechargeVendorResDTO> save(RechargeVendorReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		RechargeVendor z = reqDto.getBean();
	//	z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new RechargeVendorResDTO(z));
	}

	@Transactional
	@Override
	public Response<RechargeVendorResDTO> update(RechargeVendorReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("RechargeVendor ID required");
		
		RechargeVendor z = reqDto.getBean();
		RechargeVendor exist = null;
		Optional<RechargeVendor> o = rechargeVendorRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found RechargeVendor");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new RechargeVendorResDTO(exist));
	}

	@Transactional
	@Override
	public Response<RechargeVendorResDTO> delete(RechargeVendorReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("RechargeVendor ID required");

		RechargeVendor z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<RechargeVendorResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("RechargeVendor ID required");

		RechargeVendor exist = null;
		Optional<RechargeVendor> o = rechargeVendorRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found RechargeVendor");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<RechargeVendorResDTO> getAll() throws ServiceException{
		List<RechargeVendor> list = rechargeVendorRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("RechargeVendor list not found");
		return getSuccessResponse("Found RechargeVendor", list.stream().map(data -> new ModelMapper().map(data, RechargeVendorResDTO.class)).collect(Collectors.toList()));
	}

}
