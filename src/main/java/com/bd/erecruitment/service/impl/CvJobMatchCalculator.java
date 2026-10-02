package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.res.CvMatchResDTO;
import com.bd.erecruitment.dto.res.CvMatchResDTO.Component;
import com.bd.erecruitment.entity.CandidateProfile;
import com.bd.erecruitment.entity.JobCircular;
import com.bd.erecruitment.entity.User;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CvJobMatchCalculator {

	private static final int WEIGHT_SKILLS = 45;
	private static final int WEIGHT_EXPERIENCE = 25;
	private static final int WEIGHT_EDUCATION = 15;
	private static final int WEIGHT_KEYWORDS = 15;

	private static final int STRONG_THRESHOLD = 75;
	private static final int MODERATE_THRESHOLD = 50;
	private static final int MAX_KEYWORDS = 25;
	private static final int MIN_KEYWORDS = 3;
	private static final double DAYS_PER_YEAR = 365.25;

	private static final String[] EDUCATION_NAMES = {"None", "Diploma", "Bachelor", "Master", "PhD"};
	private static final Pattern[] EDUCATION_PATTERNS = {
		null,
		Pattern.compile("\\b(diploma|hsc|higher secondary|a[- ]level)\\b"),
		Pattern.compile("\\b(bachelor|bachelors|bsc|b\\.sc|bba|b\\.a|beng|b\\.eng|btech|b\\.tech|undergraduate|graduate|honours|honors|degree)\\b"),
		Pattern.compile("\\b(master|masters|msc|m\\.sc|mba|mphil|m\\.phil|postgraduate|post-graduate)\\b"),
		Pattern.compile("\\b(phd|ph\\.d|doctorate|doctoral)\\b")
	};

	private static final Pattern YEARS = Pattern.compile("(\\d+(?:\\.\\d+)?)");
	private static final Pattern WORD = Pattern.compile("[a-z][a-z0-9+#.]{3,}");
	private static final Set<String> STOPWORDS = Set.of(
		"with", "have", "from", "that", "this", "will", "must", "should", "able", "ability", "strong", "good", "excellent",
		"knowledge", "experience", "experiences", "years", "year", "work", "working", "skills", "skill", "required", "requirement",
		"requirements", "candidate", "candidates", "preferred", "plus", "least", "minimum", "such", "also", "other", "including",
		"etc", "and", "the", "for", "are", "you", "your", "our", "team", "teams", "demonstrated", "proven", "related", "relevant",
		"understanding", "familiarity", "within", "using", "use", "well", "both", "more", "than", "into", "their", "they",
		"who", "applicants", "applicant", "degree", "bachelor", "master", "diploma", "graduate", "field", "equivalent", "preferably");

	private CvJobMatchCalculator() {}

	public static CvMatchResDTO calculate(JobCircular job, CandidateProfile profile, User account) {
		CvMatchResDTO result = new CvMatchResDTO();
		result.setProfileCompletenessPercent(ProfileCompletenessCalculator.calculate(profile, account).getPercent());

		String corpus = corpus(profile).toLowerCase(Locale.ROOT);
		Set<String> corpusTokens = tokens(corpus);
		double earned = 0;
		int applicableWeight = 0;

		List<String> required = splitSkills(job.getSkills());
		if (!required.isEmpty()) {
			List<String> candidateSkills = profile.getSkills() == null ? List.of() : profile.getSkills().stream()
				.map(s -> StringUtils.trimToEmpty(s.getName()).toLowerCase(Locale.ROOT)).filter(StringUtils::isNotEmpty).toList();
			for (String skill : required) {
				(skillMatches(skill, candidateSkills, corpus) ? result.getMatchedSkills() : result.getMissingSkills()).add(skill);
			}
			double fraction = (double) result.getMatchedSkills().size() / required.size();
			earned += fraction * WEIGHT_SKILLS;
			applicableWeight += WEIGHT_SKILLS;
			result.getComponents().add(new Component("skills", WEIGHT_SKILLS, true, toScore(fraction)));
		} else {
			result.getComponents().add(new Component("skills", WEIGHT_SKILLS, false, 0));
		}

		Double requiredYears = requiredYears(job.getExperience());
		double candidateYears = candidateYears(profile);
		result.setRequiredYears(requiredYears);
		result.setCandidateYears(candidateYears);
		if (requiredYears != null && requiredYears > 0) {
			double fraction = Math.min(1.0, candidateYears / requiredYears);
			earned += fraction * WEIGHT_EXPERIENCE;
			applicableWeight += WEIGHT_EXPERIENCE;
			result.getComponents().add(new Component("experience", WEIGHT_EXPERIENCE, true, toScore(fraction)));
		} else {
			result.getComponents().add(new Component("experience", WEIGHT_EXPERIENCE, false, 0));
		}

		int requiredLevel = requiredEducationLevel(job.getJobRequirement());
		int candidateLevel = candidateEducationLevel(profile);
		result.setRequiredEducation(requiredLevel > 0 ? EDUCATION_NAMES[requiredLevel] : null);
		result.setCandidateEducation(EDUCATION_NAMES[candidateLevel]);
		if (requiredLevel > 0) {
			double fraction = Math.min(1.0, (double) candidateLevel / requiredLevel);
			earned += fraction * WEIGHT_EDUCATION;
			applicableWeight += WEIGHT_EDUCATION;
			result.getComponents().add(new Component("education", WEIGHT_EDUCATION, true, toScore(fraction)));
		} else {
			result.getComponents().add(new Component("education", WEIGHT_EDUCATION, false, 0));
		}

		Set<String> keywords = keywords(job.getJobRequirement());
		result.setKeywordsTotal(keywords.size());
		if (keywords.size() >= MIN_KEYWORDS) {
			int matched = (int) keywords.stream().filter(corpusTokens::contains).count();
			result.setKeywordsMatched(matched);
			double fraction = (double) matched / keywords.size();
			earned += fraction * WEIGHT_KEYWORDS;
			applicableWeight += WEIGHT_KEYWORDS;
			result.getComponents().add(new Component("keywords", WEIGHT_KEYWORDS, true, toScore(fraction)));
		} else {
			result.getComponents().add(new Component("keywords", WEIGHT_KEYWORDS, false, 0));
		}

		result.setScorable(applicableWeight > 0);
		result.setPercent(applicableWeight > 0 ? (int) Math.round(earned / applicableWeight * 100) : 0);
		result.setLevel(!result.isScorable() ? "UNKNOWN"
			: result.getPercent() >= STRONG_THRESHOLD ? "STRONG"
			: result.getPercent() >= MODERATE_THRESHOLD ? "MODERATE" : "WEAK");
		return result;
	}

	private static int toScore(double fraction) {
		return (int) Math.round(fraction * 100);
	}

	private static List<String> splitSkills(String skills) {
		if (StringUtils.isBlank(skills)) return List.of();
		Set<String> unique = new LinkedHashSet<>();
		for (String part : skills.split("[,;|\\n]")) {
			String skill = part.trim();
			if (!skill.isEmpty()) unique.add(skill);
		}
		return new ArrayList<>(unique);
	}

	private static boolean skillMatches(String requiredSkill, List<String> candidateSkills, String corpus) {
		String required = requiredSkill.toLowerCase(Locale.ROOT);
		for (String candidate : candidateSkills) {
			if (candidate.equals(required)) return true;
			if (candidate.length() >= 3 && required.length() >= 3 && (candidate.contains(required) || required.contains(candidate))) return true;
		}
		return Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(required) + "(?![a-z0-9+#])").matcher(corpus).find();
	}

	private static Double requiredYears(String experience) {
		if (StringUtils.isBlank(experience)) return null;
		Matcher matcher = YEARS.matcher(experience);
		return matcher.find() ? Double.valueOf(matcher.group(1)) : null;
	}

	private static double candidateYears(CandidateProfile profile) {
		if (profile.getWorkExperience() == null) return 0;
		long now = System.currentTimeMillis();
		List<long[]> ranges = new ArrayList<>();
		profile.getWorkExperience().forEach(w -> {
			if (w.getStartDate() == null) return;
			long start = w.getStartDate().getTime();
			Date endDate = w.getEndDate();
			long end = w.isCurrent() || endDate == null ? now : endDate.getTime();
			if (end > start) ranges.add(new long[]{start, end});
		});
		ranges.sort(Comparator.comparingLong(r -> r[0]));

		double totalMillis = 0;
		long[] open = null;
		for (long[] range : ranges) {
			if (open == null) {
				open = range.clone();
			} else if (range[0] <= open[1]) {
				open[1] = Math.max(open[1], range[1]);
			} else {
				totalMillis += open[1] - open[0];
				open = range.clone();
			}
		}
		if (open != null) totalMillis += open[1] - open[0];
		double years = totalMillis / (DAYS_PER_YEAR * 24 * 60 * 60 * 1000);
		return Math.round(years * 10) / 10.0;
	}

	private static int requiredEducationLevel(String requirement) {
		if (StringUtils.isBlank(requirement)) return 0;
		String text = requirement.toLowerCase(Locale.ROOT);
		Set<Integer> found = new TreeSet<>();
		for (int level = 1; level < EDUCATION_PATTERNS.length; level++) {
			if (EDUCATION_PATTERNS[level].matcher(text).find()) found.add(level);
		}
		return found.isEmpty() ? 0 : found.iterator().next();
	}

	private static int candidateEducationLevel(CandidateProfile profile) {
		if (profile.getEducation() == null) return 0;
		int best = 0;
		for (var education : profile.getEducation()) {
			String text = (StringUtils.trimToEmpty(education.getDegree()) + " " + StringUtils.trimToEmpty(education.getFieldOfStudy())).toLowerCase(Locale.ROOT);
			for (int level = EDUCATION_PATTERNS.length - 1; level > best; level--) {
				if (EDUCATION_PATTERNS[level].matcher(text).find()) {
					best = level;
					break;
				}
			}
		}
		return best;
	}

	private static Set<String> keywords(String requirement) {
		Set<String> keywords = new LinkedHashSet<>();
		if (StringUtils.isBlank(requirement)) return keywords;
		Matcher matcher = WORD.matcher(requirement.toLowerCase(Locale.ROOT));
		while (matcher.find() && keywords.size() < MAX_KEYWORDS) {
			String word = stripTrailingDots(matcher.group());
			if (word.length() >= 4 && !STOPWORDS.contains(word)) keywords.add(word);
		}
		return keywords;
	}

	private static Set<String> tokens(String text) {
		Set<String> tokens = new LinkedHashSet<>();
		Matcher matcher = WORD.matcher(text);
		while (matcher.find()) tokens.add(stripTrailingDots(matcher.group()));
		return tokens;
	}

	private static String stripTrailingDots(String word) {
		return StringUtils.stripEnd(word, ".");
	}

	private static String corpus(CandidateProfile profile) {
		StringBuilder text = new StringBuilder();
		append(text, profile.getHeadline(), profile.getSummary());
		if (profile.getSkills() != null) profile.getSkills().forEach(s -> append(text, s.getName()));
		if (profile.getWorkExperience() != null) profile.getWorkExperience().forEach(w -> append(text, w.getTitle(), w.getOrganizationName(), w.getDescription()));
		if (profile.getEducation() != null) profile.getEducation().forEach(e -> append(text, e.getDegree(), e.getFieldOfStudy(), e.getInstitution()));
		if (profile.getCertifications() != null) profile.getCertifications().forEach(c -> append(text, c.getName(), c.getIssuer()));
		if (profile.getProjects() != null) profile.getProjects().forEach(p -> append(text, p.getName(), p.getDescription()));
		if (profile.getLanguages() != null) profile.getLanguages().forEach(l -> append(text, l.getName()));
		return text.toString();
	}

	private static void append(StringBuilder builder, String... parts) {
		for (String part : parts) {
			if (StringUtils.isNotBlank(part)) builder.append(part).append(' ');
		}
	}
}
