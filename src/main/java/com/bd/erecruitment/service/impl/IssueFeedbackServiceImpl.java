package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.IssueFeedbackReqDTO;
import com.asl.issuetrack.dto.res.IssueFeedbackResDTO;
import com.asl.issuetrack.entity.IssueDetails;
import com.asl.issuetrack.entity.IssueFeedback;
import com.asl.issuetrack.model.IssueFeedbackModel;
import com.asl.issuetrack.repository.IssueFeedbackRepo;
import com.asl.issuetrack.service.IssueFeedbackService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.ImageUtils;
import com.asl.issuetrack.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class IssueFeedbackServiceImpl extends AbstractBaseService<IssueFeedback, IssueFeedbackResDTO, IssueFeedbackReqDTO> implements IssueFeedbackService<IssueFeedbackResDTO, IssueFeedbackReqDTO> {

	private IssueFeedbackRepo issueFeedbackRepo;

	IssueFeedbackServiceImpl(IssueFeedbackRepo issueFeedbackRepo){
		super(issueFeedbackRepo);
		this.issueFeedbackRepo = issueFeedbackRepo;
	}

	@Override
	public Response<IssueFeedbackResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<IssueFeedback> o = issueFeedbackRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("IssueFeedback found", new IssueFeedbackResDTO(o.get())) : getErrorResponse("IssueFeedback not found");
	}

	@Transactional
	@Override
	public Response<IssueFeedbackResDTO> save(IssueFeedbackReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		
		IssueFeedback z = reqDto.getBean();		
		z.setIssueDetails(reqDto.getIssueDetailsReqDTO().getBean());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new IssueFeedbackResDTO(z));
	}

	@Transactional
	@Override
	public Response<IssueFeedbackResDTO> update(IssueFeedbackReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("IssueFeedback ID required");
		
		IssueFeedback z = reqDto.getBean();
		IssueFeedback exist = null;
		Optional<IssueFeedback> o = issueFeedbackRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found IssueFeedback");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new IssueFeedbackResDTO(exist));
	}

	@Transactional
	@Override
	public Response<IssueFeedbackResDTO> delete(IssueFeedbackReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("IssueFeedback ID required");

		IssueFeedback z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<IssueFeedbackResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("IssueFeedback ID required");

		IssueFeedback exist = null;
		Optional<IssueFeedback> o = issueFeedbackRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found IssueFeedback");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<IssueFeedbackResDTO> getAll() throws ServiceException{
		List<IssueFeedback> list = issueFeedbackRepo.findAllByDeleted(false, Sort.by(Sort.Direction.DESC, "id"));
		if (list == null || list.isEmpty()) return getErrorResponse("IssueFeedback list not found");
		return getSuccessResponse("Found IssueFeedback", list.stream().map(data -> new ModelMapper().map(data, IssueFeedbackResDTO.class)).collect(Collectors.toList()));
	}

	@Override
	public Response<IssueFeedbackResDTO> upload(String issueFeedbackReqDTO, MultipartFile file) throws IOException {
		IssueFeedbackModel jsonData = new IssueFeedbackModel();
		IssueFeedback data = new IssueFeedback();
		IssueDetails cdata = new IssueDetails();
		
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			jsonData = objectMapper.readValue(issueFeedbackReqDTO, IssueFeedbackModel.class);
		} catch (IOException err) {
			System.out.printf("Error", err.toString());
		}
		
		if(file != null) {
			if (file.getSize() > 5000000) return getErrorResponse("Your selected file is too large. It should be less than 5 MB.");
			data.setFilename(file.getOriginalFilename());
			data.setFiletype(file.getContentType());
			data.setFileData(ImageUtils.compressImage(file.getBytes()));
		}
		if(jsonData != null) {
			cdata.setId(jsonData.getIssueDetailsReqDTO().getId());
			data.setDetails(jsonData.getDetails());
			data.setIssueDetails(cdata);
		}
		
		data = createEntity(data);
		if (data == null || data.getId() == null) return getErrorResponse("Can't save");
	
		return getSuccessResponse("Saved successfully", new IssueFeedbackResDTO(data));
		
	}

	@Override
	public IssueFeedback download(Long id) {
		Optional<IssueFeedback> dbDocumentData = issueFeedbackRepo.findByIdAndDeleted(id, false);

		if (!dbDocumentData.isPresent()) return null;
		if (dbDocumentData.get().getFiletype() == null || dbDocumentData.get().getFileData() == null) return null;
		
		IssueFeedback data = new IssueFeedback();
		data.setFiletype(dbDocumentData.get().getFiletype());
		data.setFileData(ImageUtils.decompressImage(dbDocumentData.get().getFileData()));

        return data;
	}
	
}
