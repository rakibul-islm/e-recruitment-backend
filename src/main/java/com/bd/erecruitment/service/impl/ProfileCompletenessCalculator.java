package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.ProfileCompletenessResDTO;
import com.bd.erecruitment.dto.res.ProfileCompletenessResDTO.Section;
import com.bd.erecruitment.entity.CandidateProfile;
import com.bd.erecruitment.entity.User;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public final class ProfileCompletenessCalculator {

	public static final int READY_THRESHOLD = 80;
	private static final int GOOD_THRESHOLD = 50;

	private ProfileCompletenessCalculator() {}

	// phone and address live on the user account (the CV prints them from there), so the account is passed in; it may be null
	public static ProfileCompletenessResDTO calculate(CandidateProfile profile, User account) {
		List<Section> sections = new ArrayList<>();

		boolean hasPhone = account != null ? has(account.getMobile()) || has(account.getPhone()) : has(profile.getPhone());
		boolean hasAddress = account != null ? has(account.getAddress()) : has(profile.getAddress());
		int basic = (has(profile.getHeadline()) ? 10 : 0) + (has(profile.getSummary()) ? 10 : 0)
			+ (hasPhone ? 5 : 0) + (hasAddress ? 5 : 0);
		sections.add(new Section("basicInfo", 30, basic));

		sections.add(new Section("workExperience", 20, tiered(profile.getWorkExperience(),
			w -> has(w.getTitle()) && has(w.getOrganizationName()) && w.getStartDate() != null, 20, 10)));
		sections.add(new Section("education", 15, tiered(profile.getEducation(),
			e -> has(e.getInstitution()) && has(e.getDegree()), 15, 8)));

		long skills = profile.getSkills() == null ? 0 : profile.getSkills().stream().filter(s -> has(s.getName())).count();
		sections.add(new Section("skills", 15, skills >= 3 ? 15 : skills > 0 ? 8 : 0));

		sections.add(new Section("languages", 5, tiered(profile.getLanguages(), l -> has(l.getName()), 5, 5)));
		sections.add(new Section("certifications", 5, tiered(profile.getCertifications(), c -> has(c.getName()), 5, 5)));
		sections.add(new Section("projects", 5, tiered(profile.getProjects(), p -> has(p.getName()), 5, 5)));
		sections.add(new Section("links", 5, has(profile.getLinkedinUrl()) || has(profile.getPortfolioUrl()) ? 5 : 0));

		int percent = sections.stream().mapToInt(Section::getEarned).sum();
		String level = percent >= READY_THRESHOLD ? "COMPLETE" : percent >= GOOD_THRESHOLD ? "GOOD" : "INCOMPLETE";
		return new ProfileCompletenessResDTO(percent, level, percent >= READY_THRESHOLD, sections);
	}

	private static boolean has(String value) {
		return StringUtils.isNotBlank(value);
	}

	private static <T> int tiered(Collection<T> items, Predicate<T> isFull, int fullPoints, int anyPoints) {
		if (items == null || items.isEmpty()) return 0;
		return items.stream().anyMatch(isFull) ? fullPoints : anyPoints;
	}
}
