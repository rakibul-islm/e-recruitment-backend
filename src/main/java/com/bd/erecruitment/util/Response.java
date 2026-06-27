package com.bd.erecruitment.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response<R> {

	private int code;
	private boolean success;
	private String message;
	private R obj;
	private List<R> list;
	private Page<R> page;
}
