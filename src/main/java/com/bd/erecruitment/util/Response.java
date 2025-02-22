package com.bd.erecruitment.util;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@Data
public class Response<R> {

	private boolean success = true;
	private String message;

	private Map<String, R> model;
	private List<R> list;
	private R obj;
	private Page<R> page;
}
