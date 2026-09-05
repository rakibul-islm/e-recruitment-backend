package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.entity.*;
import com.bd.erecruitment.repository.GeneratedCvRepo;
import com.bd.erecruitment.service.CvGenerationService;
import com.bd.erecruitment.service.StorageService;
import com.bd.erecruitment.util.ImageUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.List;

// Single built-in layout for now ("classic") - see CvGenerationService. templateKey is recorded
// per generation so more layouts can be added later without touching the schema.
@Service
@RequiredArgsConstructor
public class CvGenerationServiceImpl implements CvGenerationService {

	private static final String TEMPLATE_KEY = "classic";
	private static final SimpleDateFormat MONTH_YEAR = new SimpleDateFormat("MMM yyyy");

	private final StorageService storageService;
	private final GeneratedCvRepo generatedCvRepo;
	private final HtmlToPdfRenderer pdfRenderer;

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	@Override
	public GeneratedCv generate(User user, CandidateProfile profile) {
		String html = buildHtml(user, profile);
		byte[] pdf = pdfRenderer.render(html);

		String filename = "CV-" + safe(user.getFullName()).replaceAll("\\s+", "_") + ".pdf";
		StoredFile stored = storageService.store(filename, "application/pdf", pdf);

		GeneratedCv generatedCv = new GeneratedCv()
			.setCandidateProfileId(profile.getId())
			.setTemplateKey(TEMPLATE_KEY)
			.setStoredFileId(stored.getId())
			.setGeneratedOn(new Date());
		return generatedCvRepo.save(setAudit(generatedCv, user.getEmail()));
	}

	private GeneratedCv setAudit(GeneratedCv cv, String actor) {
		Date now = new Date();
		cv.setCreatedBy(actor).setCreatedOn(now);
		cv.setUpdatedBy(actor).setUpdatedOn(now);
		cv.setDeleted(false);
		return cv;
	}

	private String buildHtml(User user, CandidateProfile profile) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><meta charset='UTF-8'/><style>")
			// @page margin reserves the footer's strip as part of the page box itself, and the
			// @bottom-center margin box lays out the running(footer) element inside it - unlike a
			// plain position:fixed overlay, content can never flow underneath it.
			.append("@page{margin:30px 30px 55px 30px;@bottom-center{content:element(footer);}}")
			.append("body{font-family:'Helvetica',sans-serif;color:#1f2937;font-size:11px;margin:0;line-height:1.5;}")
			.append("h1{font-size:25px;margin:0 0 4px 0;color:#032967;letter-spacing:0.2px;}")
			.append(".headline{display:inline-block;color:#1e4d8f;font-size:11.5px;font-weight:bold;background:#eef3fb;border:1px solid #dbe4f3;border-radius:10px;padding:3px 12px;margin-bottom:10px;}")
			.append(".cv-header{overflow:hidden;background:#f4f7fc;border:1px solid #dce6f7;border-left:4px solid #032967;border-radius:8px;padding:20px 24px;margin-bottom:24px;}")
			.append(".cv-photo{float:right;width:80px;height:80px;margin-left:20px;border-radius:50%;border:3px solid #ffffff;}")
			.append(".cv-header-text{overflow:hidden;}")
			.append(".contact{font-size:10px;color:#4b5563;margin-top:2px;}")
			.append(".contact span{display:inline-block;margin:0 14px 4px 0;}")
			// break-after avoid keeps a heading from being stranded alone at the bottom of a page.
			.append("h2{font-size:12.5px;text-transform:uppercase;letter-spacing:0.8px;color:#032967;border-left:3px solid #032967;border-bottom:1px solid #e2e8f5;padding:0 0 6px 8px;margin-top:22px;margin-bottom:12px;page-break-after:avoid;break-after:avoid;}")
			// break-inside avoid keeps a single entry from being split across two pages - if it
			// doesn't fit in the remaining space, the whole entry moves to the next page instead.
			.append(".item{overflow:hidden;margin-bottom:13px;padding-left:12px;border-left:2px solid #e2e8f5;page-break-inside:avoid;break-inside:avoid;}")
			.append(".item-title{font-weight:bold;font-size:12px;color:#111827;}")
			.append(".item-sub{color:#5b6b85;font-size:10.5px;margin-top:1px;}")
			.append(".item-dates{float:right;color:#1e4d8f;font-weight:bold;font-size:9.5px;background:#eef3fb;border:1px solid #dbe4f3;border-radius:9px;padding:2px 10px;}")
			.append(".item-desc{margin-top:5px;white-space:pre-line;color:#374151;}")
			.append(".tag{display:inline-block;background:#eef3fb;color:#1e4d8f;font-weight:bold;border:1px solid #dbe4f3;border-radius:3px;padding:3px 10px;margin:0 6px 6px 0;font-size:10px;}")
			.append(".cv-footer{position:running(footer);padding-top:8px;border-top:1px solid #e5e7eb;text-align:center;color:#9ca3af;font-size:9px;}")
			.append(".cv-footer-brand{color:#1e4d8f;font-weight:bold;letter-spacing:0.4px;}")
			.append(".cv-footer-link{color:#9ca3af;text-decoration:none;}")
			.append("</style></head><body>");

		// A running() element only takes effect on pages laid out after its point in document
		// order, so it has to appear before the first page's content - not trailing at the end -
		// or the first page renders with no footer at all.
		sb.append("<div class='cv-footer'>Powered by <span class='cv-footer-brand'>E-RECRUITMENT</span>");
		if (StringUtils.isNotBlank(frontendBaseUrl)) {
			sb.append(" · <a class='cv-footer-link' href='").append(esc(frontendBaseUrl)).append("'>").append(esc(displayUrl(frontendBaseUrl))).append("</a>");
		}
		sb.append("</div>");

		sb.append("<div class='cv-header'>");
		sb.append("<img class='cv-photo' src='data:image/png;base64,").append(photoBase64(user)).append("' alt='' />");
		sb.append("<div class='cv-header-text'>");
		sb.append("<h1>").append(esc(user.getFullName())).append("</h1>");
		if (StringUtils.isNotBlank(profile.getHeadline())) {
			sb.append("<div class='headline'>").append(esc(profile.getHeadline())).append("</div>");
		}
		sb.append("<div class='contact'>");
		// Phone/address are account-level data (inherited from the User, not the CV-specific
		// CandidateProfile) - mobile is treated as the primary number, phone as secondary.
		List<String> contactParts = List.of(
			StringUtils.defaultString(user.getEmail()),
			StringUtils.defaultString(user.getMobile()),
			StringUtils.defaultString(user.getPhone()),
			StringUtils.defaultString(user.getAddress()),
			StringUtils.defaultString(profile.getLinkedinUrl()),
			StringUtils.defaultString(profile.getPortfolioUrl())
		);
		for (String part : contactParts) {
			if (StringUtils.isNotBlank(part)) sb.append("<span>").append(esc(part)).append("</span>");
		}
		sb.append("</div>");
		sb.append("</div></div>");

		if (StringUtils.isNotBlank(profile.getSummary())) {
			sb.append("<h2>Summary</h2><div>").append(esc(profile.getSummary())).append("</div>");
		}

		if (!isEmpty(profile.getWorkExperience())) {
			sb.append("<h2>Work Experience</h2>");
			for (WorkExperienceItem w : profile.getWorkExperience()) {
				sb.append("<div class='item'>")
					.append("<span class='item-dates'>").append(dateRange(w.getStartDate(), w.getEndDate(), w.isCurrent())).append("</span>")
					.append("<div class='item-title'>").append(esc(w.getTitle())).append("</div>")
					.append("<div class='item-sub'>").append(esc(joinNonBlank(w.getCompanyName(), w.getLocation()))).append("</div>");
				if (StringUtils.isNotBlank(w.getDescription())) sb.append("<div class='item-desc'>").append(esc(w.getDescription())).append("</div>");
				sb.append("</div>");
			}
		}

		if (!isEmpty(profile.getEducation())) {
			sb.append("<h2>Education</h2>");
			for (EducationItem ed : profile.getEducation()) {
				sb.append("<div class='item'>")
					.append("<span class='item-dates'>").append(dateRange(ed.getStartDate(), ed.getEndDate(), Boolean.TRUE.equals(ed.getCurrent()))).append("</span>")
					.append("<div class='item-title'>").append(esc(joinNonBlank(ed.getDegree(), ed.getFieldOfStudy()))).append("</div>")
					.append("<div class='item-sub'>").append(esc(ed.getInstitution())).append(StringUtils.isNotBlank(ed.getGrade()) ? " · " + esc(ed.getGrade()) : "").append("</div>")
					.append("</div>");
			}
		}

		if (!isEmpty(profile.getSkills())) {
			sb.append("<h2>Skills</h2><div>");
			for (SkillItem s : profile.getSkills()) {
				String label = StringUtils.isNotBlank(s.getLevel()) ? s.getName() + " (" + s.getLevel() + ")" : s.getName();
				sb.append("<span class='tag'>").append(esc(label)).append("</span>");
			}
			sb.append("</div>");
		}

		if (!isEmpty(profile.getCertifications())) {
			sb.append("<h2>Certifications</h2>");
			for (CertificationItem c : profile.getCertifications()) {
				sb.append("<div class='item'>")
					.append("<span class='item-dates'>").append(c.getDate() != null ? esc(MONTH_YEAR.format(c.getDate())) : "").append("</span>")
					.append("<div class='item-title'>").append(esc(c.getName())).append("</div>")
					.append("<div class='item-sub'>").append(esc(c.getIssuer())).append("</div>")
					.append("</div>");
			}
		}

		if (!isEmpty(profile.getProjects())) {
			sb.append("<h2>Projects</h2>");
			for (ProjectItem p : profile.getProjects()) {
				sb.append("<div class='item'>")
					.append("<div class='item-title'>").append(esc(p.getName())).append("</div>");
				if (StringUtils.isNotBlank(p.getUrl())) sb.append("<div class='item-sub'>").append(esc(p.getUrl())).append("</div>");
				if (StringUtils.isNotBlank(p.getDescription())) sb.append("<div class='item-desc'>").append(esc(p.getDescription())).append("</div>");
				sb.append("</div>");
			}
		}

		if (!isEmpty(profile.getLanguages())) {
			sb.append("<h2>Languages</h2><div>");
			for (LanguageItem l : profile.getLanguages()) {
				String label = StringUtils.isNotBlank(l.getProficiency()) ? l.getName() + " (" + l.getProficiency() + ")" : l.getName();
				sb.append("<span class='tag'>").append(esc(label)).append("</span>");
			}
			sb.append("</div>");
		}

		sb.append("</body></html>");
		return sb.toString();
	}

	private String dateRange(Date start, Date end, boolean current) {
		String startStr = start != null ? MONTH_YEAR.format(start) : "";
		String endStr = current ? "Present" : (end != null ? MONTH_YEAR.format(end) : "");
		if (StringUtils.isBlank(startStr) && StringUtils.isBlank(endStr)) return "";
		return esc(startStr + " - " + endStr);
	}

	private String joinNonBlank(String... parts) {
		return String.join(", ", java.util.Arrays.stream(parts).filter(StringUtils::isNotBlank).toList());
	}

	private boolean isEmpty(List<?> list) {
		return list == null || list.isEmpty();
	}

	private String safe(String value) {
		return StringUtils.defaultIfBlank(value, "candidate");
	}

	private String displayUrl(String url) {
		return url.replaceFirst("^https?://", "").replaceFirst("/$", "");
	}

	private String photoBase64(User user) {
		byte[] fileData = user.getFileData();
		return fileData != null && fileData.length > 0
			? Base64.getEncoder().encodeToString(fileData)
			: ImageUtils.DEFAULT_AVATAR_BASE64;
	}

	private String esc(String value) {
		if (value == null) return "";
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
