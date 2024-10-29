package com.bd.erecruitment.util;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Data
public class Response<R> {

	private boolean success = true;
	private boolean info = false;
	private boolean warning = false;

	private String code;
	private String message;

	private Map<String, R> model;
	private List<R> list;
	private R obj;
	private Page<R> page;
}
