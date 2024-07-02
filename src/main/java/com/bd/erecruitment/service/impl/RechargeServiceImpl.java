package com.bd.erecruitment.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asl.issuetrack.dto.req.RechargeReqDTO;
import com.asl.issuetrack.dto.res.RechargeResDTO;
import com.asl.issuetrack.entity.Recharge;
import com.asl.issuetrack.repository.RechargeRepo;
import com.asl.issuetrack.service.RechargeService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;

@Service
public class RechargeServiceImpl extends AbstractBaseService<Recharge, RechargeResDTO, RechargeReqDTO> implements RechargeService<RechargeResDTO, RechargeReqDTO> {

	private RechargeRepo rechargeRepo;

	RechargeServiceImpl(RechargeRepo rechargeRepo){
		super(rechargeRepo);
		this.rechargeRepo = rechargeRepo;
	}

	@Override
	public Response<RechargeResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<Recharge> o = rechargeRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("Recharge found", new RechargeResDTO(o.get())) : getErrorResponse("Recharge not found");
	}

	@Transactional
	@Override
	public Response<RechargeResDTO> save(RechargeReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		Recharge z = reqDto.getBean();
	//	z.setUser(getLoggedInUser());
		z.setRechargeVendor(reqDto.getRechargeVendorReqDTO().getBean());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new RechargeResDTO(z));
	}

	@Transactional
	@Override
	public Response<RechargeResDTO> update(RechargeReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Recharge ID required");
		
		Recharge z = reqDto.getBean();
		Recharge exist = null;
		Optional<Recharge> o = rechargeRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found Recharge");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new RechargeResDTO(exist));
	}

	@Transactional
	@Override
	public Response<RechargeResDTO> delete(RechargeReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Recharge ID required");

		Recharge z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<RechargeResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("Recharge ID required");

		Recharge exist = null;
		Optional<Recharge> o = rechargeRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found Recharge");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<RechargeResDTO> getAll() throws ServiceException{
		List<Recharge> list = rechargeRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("Recharge list not found");
		return getSuccessResponse("Found Recharge", list.stream().map(data -> new ModelMapper().map(data, RechargeResDTO.class)).collect(Collectors.toList()));
	}

}
