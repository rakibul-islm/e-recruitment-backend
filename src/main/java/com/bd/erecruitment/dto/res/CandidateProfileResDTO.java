package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CandidateProfileResDTO extends BaseResponseDTO<CandidateProfile> {

	public CandidateProfileResDTO(CandidateProfile profile) {
		new ModelMapper().map(profile, this);
	}

	private Long userId;
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
}
