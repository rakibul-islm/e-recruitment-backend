package com.bd.erecruitment.service;

import com.asl.issuetrack.dto.res.IssueDetailsResDTO;
import com.asl.issuetrack.entity.IssueDetails;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;

public interface IssueDetailsService<R, E> extends BaseService<R, E> {
	
	public Response<IssueDetailsResDTO> upload(String issueDetailsReqDTO,MultipartFile file) throws IOException;	
	public IssueDetails download(Long id);	
	public Response<IssueDetailsResDTO> findAllByDateAndStatus(Date toDate, Date fromDate, String status) throws ServiceException;
	
}
