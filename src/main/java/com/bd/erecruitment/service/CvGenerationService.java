package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.CandidateProfile;
import com.bd.erecruitment.entity.GeneratedCv;
import com.bd.erecruitment.entity.User;

public interface CvGenerationService {

	GeneratedCv generate(User user, CandidateProfile profile);
}
