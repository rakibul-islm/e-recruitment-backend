package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.service.OtpService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;

@Service
public class OtpServiceImpl implements OtpService {

	@Value("${app.otp.expiry-minutes:10}")
	private long otpExpiryMinutes;

	@Value("${app.otp.max-attempts:5}")
	private int otpMaxAttempts;

	private final UserRepo userRepo;
	private final SecureRandom secureRandom = new SecureRandom();

	public OtpServiceImpl(UserRepo userRepo) {
		this.userRepo = userRepo;
	}

	@Override
	public String generate(User user) {
		String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
		user.setOtpCode(otp);
		user.setOtpExpiry(new Date(System.currentTimeMillis() + otpExpiryMinutes * 60_000));
		user.setOtpAttempts(0);
		return otp;
	}

	@Override
	public void validate(User user, String otp) {
		if (user == null || StringUtils.isBlank(user.getOtpCode())) throw new BadRequestException("Invalid OTP");

		if (user.getOtpAttempts() >= otpMaxAttempts) {
			user.setOtpCode(null);
			user.setOtpExpiry(null);
			user.setOtpAttempts(0);
			userRepo.save(user);
			throw new BadRequestException("Too many attempts, request a new OTP");
		}

		if (user.getOtpExpiry() == null || user.getOtpExpiry().before(new Date())) {
			user.setOtpCode(null);
			user.setOtpExpiry(null);
			user.setOtpAttempts(0);
			userRepo.save(user);
			throw new BadRequestException("OTP has expired");
		}

		if (!user.getOtpCode().equals(otp)) {
			user.setOtpAttempts(user.getOtpAttempts() + 1);
			userRepo.save(user);
			throw new BadRequestException("Invalid OTP");
		}
	}
}
