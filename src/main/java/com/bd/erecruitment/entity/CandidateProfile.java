package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

// 1:1 with User (userId, no JPA relation - same convention as User.userGroupId). Holds the
// structured data CvGenerationService renders into a downloadable CV/resume PDF.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "CANDIDATE_PROFILE")
@EqualsAndHashCode(callSuper = true)
public class CandidateProfile extends SequenceIdGenerator {

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	private String headline;

	@Column(length = 2000)
	private String summary;

	private String phone;
	private String address;
	private String linkedinUrl;
	private String portfolioUrl;

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "CANDIDATE_WORK_EXPERIENCE", joinColumns = @JoinColumn(name = "candidate_profile_id"))
	private List<WorkExperienceItem> workExperience = new ArrayList<>();

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "CANDIDATE_EDUCATION", joinColumns = @JoinColumn(name = "candidate_profile_id"))
	private List<EducationItem> education = new ArrayList<>();

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "CANDIDATE_SKILL", joinColumns = @JoinColumn(name = "candidate_profile_id"))
	private List<SkillItem> skills = new ArrayList<>();

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "CANDIDATE_CERTIFICATION", joinColumns = @JoinColumn(name = "candidate_profile_id"))
	private List<CertificationItem> certifications = new ArrayList<>();

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "CANDIDATE_LANGUAGE", joinColumns = @JoinColumn(name = "candidate_profile_id"))
	private List<LanguageItem> languages = new ArrayList<>();

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "CANDIDATE_PROJECT", joinColumns = @JoinColumn(name = "candidate_profile_id"))
	private List<ProjectItem> projects = new ArrayList<>();
}
