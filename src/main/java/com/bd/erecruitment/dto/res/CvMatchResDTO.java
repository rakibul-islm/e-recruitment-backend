package com.bd.erecruitment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CvMatchResDTO {

	private int percent;
	private String level;
	private boolean scorable;

	private List<String> matchedSkills = new ArrayList<>();
	private List<String> missingSkills = new ArrayList<>();

	private Double requiredYears;
	private double candidateYears;

	private String requiredEducation;
	private String candidateEducation;

	private int keywordsMatched;
	private int keywordsTotal;

	private int profileCompletenessPercent;
	private List<Component> components = new ArrayList<>();

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Component {
		private String key;
		private int weight;
		private boolean applicable;
		private int score;
	}
}
