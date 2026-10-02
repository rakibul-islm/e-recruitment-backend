package com.bd.erecruitment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompletenessResDTO {

	private int percent;
	private String level;
	private boolean ready;
	private List<Section> sections;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Section {
		private String key;
		private int weight;
		private int earned;
	}
}
