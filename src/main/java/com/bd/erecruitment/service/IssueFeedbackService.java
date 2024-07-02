package com.bd.erecruitment.service;

import com.asl.issuetrack.dto.res.IssueFeedbackResDTO;
import com.asl.issuetrack.entity.IssueFeedback;
import com.asl.issuetrack.util.Response;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface IssueFeedbackService<R, E> extends BaseService<R, E> {
	
	public Response<IssueFeedbackResDTO> upload(String issueFeedbackReqDTO,MultipartFile file) throws IOException;
	public IssueFeedback download(Long id);
	
}
