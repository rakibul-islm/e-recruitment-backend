package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.CandidateProfile;
import com.bd.erecruitment.entity.GeneratedCv;
import com.bd.erecruitment.entity.User;

// Renders a candidate's structured profile data into a formatted CV/resume PDF - this is what
// lets a candidate apply without needing a pre-made resume file (see StorageService for where
// the PDF ends up, GeneratedCv for the generation record).
public interface CvGenerationService {

	GeneratedCv generate(User user, CandidateProfile profile);
}
