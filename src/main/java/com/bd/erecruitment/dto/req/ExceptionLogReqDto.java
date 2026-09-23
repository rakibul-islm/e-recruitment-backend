package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.ExceptionLog;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ExceptionLogReqDto extends BaseRequestDTO<ExceptionLog> {

	private String traceId;
	private String exceptionClass;
	private int statusCode;
	private String requestUri;
	private String message;
	private String stackTrace;

	@JsonIgnore
	@Override
	public ExceptionLog getBean() {
		ExceptionLog e = new ExceptionLog();
		ModelMapperUtils.MAPPER.map(this, e);
		return e;
	}
}
