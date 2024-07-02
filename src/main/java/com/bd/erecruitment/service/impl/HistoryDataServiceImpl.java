package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.HistoryDataReqDTO;
import com.asl.issuetrack.dto.res.HistoryDataResDTO;
import com.asl.issuetrack.entity.HistoryData;
import com.asl.issuetrack.repository.HistoryDataRepo;
import com.asl.issuetrack.service.HistoryDataService;
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
public class HistoryDataServiceImpl extends AbstractBaseService<HistoryData, HistoryDataResDTO, HistoryDataReqDTO> implements HistoryDataService<HistoryDataResDTO, HistoryDataReqDTO> {

	private HistoryDataRepo historyDataRepo;

	HistoryDataServiceImpl(HistoryDataRepo historyDataRepo){
		super(historyDataRepo);
		this.historyDataRepo = historyDataRepo;
	}

	@Override
	public Response<HistoryDataResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<HistoryData> o = historyDataRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("HistoryData found", new HistoryDataResDTO(o.get())) : getErrorResponse("HistoryData not found");
	}

	@Transactional
	@Override
	public Response<HistoryDataResDTO> save(HistoryDataReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		HistoryData z = reqDto.getBean();
		z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new HistoryDataResDTO(z));
	}

	@Transactional
	@Override
	public Response<HistoryDataResDTO> update(HistoryDataReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("HistoryData ID required");
		
		HistoryData z = reqDto.getBean();
		HistoryData exist = null;
		Optional<HistoryData> o = historyDataRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found HistoryData");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new HistoryDataResDTO(exist));
	}

	@Transactional
	@Override
	public Response<HistoryDataResDTO> delete(HistoryDataReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("HistoryData ID required");

		HistoryData z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<HistoryDataResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("HistoryData ID required");

		HistoryData exist = null;
		Optional<HistoryData> o = historyDataRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found HistoryData");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<HistoryDataResDTO> getAll() throws ServiceException{
		List<HistoryData> list = historyDataRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("HistoryData list not found");
		return getSuccessResponse("Found HistoryData", list.stream().map(data -> new ModelMapper().map(data, HistoryDataResDTO.class)).collect(Collectors.toList()));
	}

}
