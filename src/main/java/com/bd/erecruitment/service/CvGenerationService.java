package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.CandidateCv;
import com.bd.erecruitment.entity.GeneratedCv;
import com.bd.erecruitment.entity.User;

public interface CvGenerationService {

	GeneratedCv generate(User user, CandidateCv profile);
}
