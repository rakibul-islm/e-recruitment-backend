package com.bd.erecruitment.service;

import com.bd.erecruitment.entity.User;

public interface OtpService {

	String generate(User user);

	void validate(User user, String otp);
}
