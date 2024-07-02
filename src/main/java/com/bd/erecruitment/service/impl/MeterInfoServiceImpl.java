package com.bd.erecruitment.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asl.issuetrack.dto.req.MeterInfoReqDTO;
import com.asl.issuetrack.dto.res.MeterInfoResDTO;
import com.asl.issuetrack.entity.MeterInfo;
import com.asl.issuetrack.repository.MeterInfoRepo;
import com.asl.issuetrack.service.MeterInfoService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;

@Service
public class MeterInfoServiceImpl extends AbstractBaseService<MeterInfo, MeterInfoResDTO, MeterInfoReqDTO> implements MeterInfoService<MeterInfoResDTO, MeterInfoReqDTO> {

	private MeterInfoRepo meterInfoRepo;

	MeterInfoServiceImpl(MeterInfoRepo meterInfoRepo){
		super(meterInfoRepo);
		this.meterInfoRepo = meterInfoRepo;
	}

	@Override
	public Response<MeterInfoResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<MeterInfo> o = meterInfoRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("MeterInfo found", new MeterInfoResDTO(o.get())) : getErrorResponse("MeterInfo not found");
	}

	@Transactional
	@Override
	public Response<MeterInfoResDTO> save(MeterInfoReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if(StringUtils.isBlank(reqDto.getMeterNo())) return getErrorResponse("Meter Number required");
		
		MeterInfo z = reqDto.getBean();
		z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new MeterInfoResDTO(z));
	}

	@Transactional
	@Override
	public Response<MeterInfoResDTO> update(MeterInfoReqDTO reqDto) throws ServiceException{

		return null;
	}

	@Transactional
	@Override
	public Response<MeterInfoResDTO> delete(MeterInfoReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("MeterInfo ID required");

		MeterInfo z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<MeterInfoResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("MeterInfo ID required");

		MeterInfo exist = null;
		Optional<MeterInfo> o = meterInfoRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found MeterInfo");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<MeterInfoResDTO> getAll() throws ServiceException{
		List<MeterInfo> list = meterInfoRepo.findAllByUserIdAndDeleted(getLoggedInUser().getId(), false);
		if (list == null || list.isEmpty()) return getErrorResponse("MeterInfo list not found");
		return getSuccessResponse("Found MeterInfo", list.stream().map(data -> new ModelMapper().map(data, MeterInfoResDTO.class)).collect(Collectors.toList()));
	}

}
