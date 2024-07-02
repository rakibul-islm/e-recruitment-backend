package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.SndReqDTO;
import com.asl.issuetrack.dto.res.SndResDTO;
import com.asl.issuetrack.entity.Snd;
import com.asl.issuetrack.repository.SndRepo;
import com.asl.issuetrack.service.SndService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SndServiceImpl extends AbstractBaseService<Snd, SndResDTO, SndReqDTO> implements SndService<SndResDTO, SndReqDTO> {

	private SndRepo sndRepo;

	SndServiceImpl(SndRepo sndRepo){
		super(sndRepo);
		this.sndRepo = sndRepo;
	}

	@Override
	public Response<SndResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<Snd> o = sndRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("Snd found", new SndResDTO(o.get())) : getErrorResponse("Snd not found");
	}

	@Transactional
	@Override
	public Response<SndResDTO> save(SndReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getCode() == null) return getErrorResponse("Snd Code required");
		if(StringUtils.isBlank(reqDto.getName())) return getErrorResponse("Name required");
		
		Snd z = reqDto.getBean();
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new SndResDTO(z));
	}

	@Transactional
	@Override
	public Response<SndResDTO> update(SndReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Snd ID required");
		if (reqDto.getCode() == null) return getErrorResponse("Snd Code required");
		if(StringUtils.isBlank(reqDto.getName())) return getErrorResponse("Name required");
		
		Snd z = reqDto.getBean();
		Snd exist = null;
		Optional<Snd> o = sndRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found Snd");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new SndResDTO(exist));
	}

	@Transactional
	@Override
	public Response<SndResDTO> delete(SndReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Snd ID required");

		Snd z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<SndResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("Snd ID required");

		Snd exist = null;
		Optional<Snd> o = sndRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found Snd");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<SndResDTO> getAll() throws ServiceException{
		List<Snd> list = sndRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("Snd list not found");
		return getSuccessResponse("Found Snd", list.stream().map(data -> new ModelMapper().map(data, SndResDTO.class)).collect(Collectors.toList()));
	}

}
