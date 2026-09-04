package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CandidateProfileReqDto extends BaseRequestDTO<CandidateProfile> {

	private String headline;
	private String summary;
	private String phone;
	private String address;
	private String linkedinUrl;
	private String portfolioUrl;

	private List<WorkExperienceItem> workExperience = new ArrayList<>();
	private List<EducationItem> education = new ArrayList<>();
	private List<SkillItem> skills = new ArrayList<>();
	private List<CertificationItem> certifications = new ArrayList<>();
	private List<LanguageItem> languages = new ArrayList<>();
	private List<ProjectItem> projects = new ArrayList<>();

	@JsonIgnore
	@Override
	public CandidateProfile getBean() {
		CandidateProfile p = new CandidateProfile();
		new ModelMapper().map(this, p);
		return p;
	}
}
