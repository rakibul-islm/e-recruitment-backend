package com.bd.erecruitment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArchivedDataResDTO {

	private List<String> columns;
	private List<Map<String, Object>> rows;
	private long totalElements;
	private int page;
	private int size;
}
