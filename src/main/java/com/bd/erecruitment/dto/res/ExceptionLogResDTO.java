package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.ExceptionLog;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExceptionLogResDTO extends BaseResponseDTO<ExceptionLog> {

	private String traceId;
	private String exceptionClass;
	private int statusCode;
	private String requestUri;
	private String message;
	private String stackTrace;
	private Date createdOn;

	public ExceptionLogResDTO(ExceptionLog log) {
		new ModelMapper().map(log, this);
	}
}
