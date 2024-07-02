package com.bd.erecruitment.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.asl.issuetrack.dto.req.IssueDetailsReqDTO;
import com.asl.issuetrack.dto.res.IssueDetailsResDTO;
import com.asl.issuetrack.entity.IssueCategory;
import com.asl.issuetrack.entity.IssueDetails;
import com.asl.issuetrack.entity.User;
import com.asl.issuetrack.enums.UserRole;
import com.asl.issuetrack.model.IssueDetailsModel;
import com.asl.issuetrack.repository.IssueDetailsRepo;
import com.asl.issuetrack.repository.IssueMasterRepo;
import com.asl.issuetrack.service.IssueDetailsService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.ImageUtils;
import com.asl.issuetrack.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class IssueDetailsServiceImpl extends AbstractBaseService<IssueDetails, IssueDetailsResDTO, IssueDetailsReqDTO> implements IssueDetailsService<IssueDetailsResDTO, IssueDetailsReqDTO> {

	private IssueDetailsRepo issueDetailsRepo;
	private IssueMasterRepo issueMasterRepo;

	IssueDetailsServiceImpl(IssueDetailsRepo issueDetailsRepo,IssueMasterRepo issueMasterRepo){
		super(issueDetailsRepo);
		this.issueDetailsRepo = issueDetailsRepo;
		this.issueMasterRepo = issueMasterRepo;
	}

	@Override
	public Response<IssueDetailsResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<IssueDetails> o = issueDetailsRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("IssueDetails found", new IssueDetailsResDTO(o.get())) : getErrorResponse("IssueDetails not found");
	}

	@Transactional
	@Override
	public Response<IssueDetailsResDTO> save(IssueDetailsReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		User user = getLoggedInUser();
		
		IssueDetails z = reqDto.getBean();
		z.setUser(user);
		if(user.getAdmin_id() == null) return getErrorResponse("Responsible Admin not found");
		z.setAdmin(getLoggedInSndBasedAdmin());
		
		if(user.getSnd_id() == null) return getErrorResponse("S&D not found");
		z.setSnd(getLoggedInUserBasedSnd());
		
		z.setStatus("Open");
		z.setIssueCategory(reqDto.getIssueCategoryReqDTO().getBean());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new IssueDetailsResDTO(z));
	}

	@Transactional
	@Override
	public Response<IssueDetailsResDTO> update(IssueDetailsReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("IssueDetails ID required");
		
		IssueDetails z = reqDto.getBean();
		IssueDetails exist = null;
		Optional<IssueDetails> o = issueDetailsRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found IssueDetails");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new IssueDetailsResDTO(exist));
	}

	@Transactional
	@Override
	public Response<IssueDetailsResDTO> delete(IssueDetailsReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("IssueDetails ID required");

		IssueDetails z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<IssueDetailsResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("IssueDetails ID required");

		IssueDetails exist = null;
		Optional<IssueDetails> o = issueDetailsRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found IssueDetails");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<IssueDetailsResDTO> getAll() throws ServiceException{
		List<IssueDetails> list;
		User user = getLoggedInUser();
		
		if(user.getRoles().equalsIgnoreCase(UserRole.ROLE_SUPER_ADMIN.name())) {
			list = issueDetailsRepo.findAllByDeleted(false, Sort.by(Sort.Direction.DESC, "id"));
		}else if(user.getRoles().equalsIgnoreCase(UserRole.ROLE_SYSTEM_ADMIN.name())){
			list = issueDetailsRepo.findAllByAdminIdAndDeleted(user.getId(), false, Sort.by(Sort.Direction.DESC, "id"));
		}else {
			list = issueDetailsRepo.findAllByUserIdAndDeleted(user.getId(), false, Sort.by(Sort.Direction.DESC, "id"));
		}
		
		if (list == null || list.isEmpty()) return getErrorResponse("IssueDetails list not found");
		return getSuccessResponse("Found IssueDetails", list.stream().map(data -> new ModelMapper().map(data, IssueDetailsResDTO.class)).collect(Collectors.toList()));
	}

	@Override
	public Response<IssueDetailsResDTO> upload(String issueDetailsReqDTO, MultipartFile file) throws IOException {
		IssueDetailsModel jsonData = new IssueDetailsModel();
		IssueDetails data = new IssueDetails();
		IssueCategory cdata = new IssueCategory();
		User user = getLoggedInUser();
		
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			jsonData = objectMapper.readValue(issueDetailsReqDTO, IssueDetailsModel.class);
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
			cdata.setId(jsonData.getIssueCategoryReqDTO().getId());
			data.setDetails(jsonData.getDetails());
			data.setStatus(jsonData.getStatus());
			data.setIssueCategory(cdata);
		}
		
		Optional<IssueCategory> o = issueMasterRepo.findByIdAndDeleted(cdata.getId(), false);
		if (o.get().isRequiredDocumnt() && data.getFilename() == null) return getErrorResponse("Document is required for "+ o.get().getName());
		
		data.setUser(user);
		if(user.getAdmin_id() == null) return getErrorResponse("Responsible Admin not found");
		data.setAdmin(getLoggedInSndBasedAdmin());
		
		if(user.getSnd_id() == null) return getErrorResponse("S&D not found");
		data.setSnd(getLoggedInUserBasedSnd());
		
		data.setStatus("Open");
		data = createEntity(data);
		if (data == null || data.getId() == null) return getErrorResponse("Can't save");
	
		return getSuccessResponse("Saved successfully", new IssueDetailsResDTO(data));
		
	}

	@Override
	public IssueDetails download(Long id) {
		Optional<IssueDetails> dbDocumentData = issueDetailsRepo.findByIdAndDeleted(id, false);

		if (!dbDocumentData.isPresent()) return null;
		if (dbDocumentData.get().getFiletype() == null || dbDocumentData.get().getFileData() == null) return null;
		
		IssueDetails data = new IssueDetails();
		data.setFiletype(dbDocumentData.get().getFiletype());
		data.setFileData(ImageUtils.decompressImage(dbDocumentData.get().getFileData()));

        return data;
	}

	@Override
	public Response<IssueDetailsResDTO> findAllByDateAndStatus(Date toDate, Date fromDate, String status) throws ServiceException {
		List<IssueDetails> list;
		if(status.equalsIgnoreCase("all")) {
			list = issueDetailsRepo.findAllByCreatedOnLessThanEqualAndCreatedOnGreaterThanEqualAndDeleted(fromDate, toDate, false, Sort.by(Sort.Direction.ASC, "id"));
		}else {
			list = issueDetailsRepo.findAllByCreatedOnLessThanEqualAndCreatedOnGreaterThanEqualAndStatusAndDeleted(fromDate, toDate, status, false, Sort.by(Sort.Direction.ASC, "id"));
		}
		
		if (list == null || list.isEmpty()) return getErrorResponse("IssueDetails list not found");
		
		return getSuccessResponse("Found IssueDetails", list.stream().map(data -> new ModelMapper().map(data, IssueDetailsResDTO.class)).collect(Collectors.toList()));
	}
	
}
