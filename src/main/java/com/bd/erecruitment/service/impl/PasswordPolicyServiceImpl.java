package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.PasswordPolicyReqDto;
import com.bd.erecruitment.dto.res.PasswordPolicyResDTO;
import com.bd.erecruitment.entity.PasswordPolicy;
import com.bd.erecruitment.repository.PasswordPolicyRepo;
import com.bd.erecruitment.seed.data.PasswordPolicyData;
import com.bd.erecruitment.service.PasswordPolicyService;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordPolicyServiceImpl extends AbstractBaseService<PasswordPolicy> implements PasswordPolicyService {

	private final PasswordPolicyRepo passwordPolicyRepo;

	// Lazily filled, kept in sync by update() so validatePassword() avoids a DB hit each call.
	private volatile PasswordPolicy cachedPolicy;

	PasswordPolicyServiceImpl(PasswordPolicyRepo passwordPolicyRepo) {
		super(passwordPolicyRepo);
		this.passwordPolicyRepo = passwordPolicyRepo;
	}

	@Transactional
	@Override
	public Response<PasswordPolicyResDTO> find() {
		return getSuccessResponse("Found", new PasswordPolicyResDTO(getActivePolicy()));
	}

	@Transactional
	@Override
	public Response<PasswordPolicyResDTO> update(PasswordPolicyReqDto reqDto) {
		validateForm(reqDto);
		PasswordPolicy existing = captureSnapshot(findPolicyOrThrow());
		existing.setMinLength(reqDto.getMinLength())
			.setMaxLength(reqDto.getMaxLength())
			.setRequireUppercase(reqDto.isRequireUppercase())
			.setRequireLowercase(reqDto.isRequireLowercase())
			.setRequireDigit(reqDto.isRequireDigit())
			.setRequireSpecialChar(reqDto.isRequireSpecialChar())
			.setDisallowUserInfoInPassword(reqDto.isDisallowUserInfoInPassword());
		PasswordPolicy updated = updateEntity(existing);
		cachedPolicy = updated;
		return getSuccessResponse("Updated successfully", new PasswordPolicyResDTO(updated));
	}

	@Transactional
	@Override
	public void validatePassword(String password, String email, String fullName) {
		if (StringUtils.isBlank(password)) returnErrorException("Password required");
		PasswordPolicy policy = getActivePolicy();

		List<String> errors = new ArrayList<>();

		if (password.length() < policy.getMinLength())
			errors.add("min " + policy.getMinLength() + " characters");
		if (password.length() > policy.getMaxLength())
			errors.add("max " + policy.getMaxLength() + " characters");
		if (policy.isRequireUppercase() && password.chars().noneMatch(Character::isUpperCase))
			errors.add("1 uppercase letter");
		if (policy.isRequireLowercase() && password.chars().noneMatch(Character::isLowerCase))
			errors.add("1 lowercase letter");
		if (policy.isRequireDigit() && password.chars().noneMatch(Character::isDigit))
			errors.add("1 digit");
		if (policy.isRequireSpecialChar() && password.chars().allMatch(Character::isLetterOrDigit))
			errors.add("1 special character");

		if (policy.isDisallowUserInfoInPassword()) {
			String lowerPwd = password.toLowerCase();
			if (StringUtils.isNotBlank(email)) {
				String localPart = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
				if (localPart.length() >= 3 && lowerPwd.contains(localPart.toLowerCase()))
					errors.add("must not contain your email");
			}
			if (StringUtils.isNotBlank(fullName)) {
				for (String part : fullName.trim().split("\\s+")) {
					if (part.length() >= 3 && lowerPwd.contains(part.toLowerCase())) {
						errors.add("must not contain your name");
						break;
					}
				}
			}
		}

		if (!errors.isEmpty()) returnErrorException("Password requires: " + String.join(", ", errors));
	}

	private void validateForm(PasswordPolicyReqDto reqDto) {
		if (reqDto.getMinLength() <= 0) returnErrorException("Minimum length must be greater than 0");
		if (reqDto.getMaxLength() < reqDto.getMinLength()) returnErrorException("Maximum length must be greater than or equal to minimum length");
	}

	// A missing row means PasswordPolicySeeder didn't run — surface it rather than fabricating one.
	private PasswordPolicy findPolicyOrThrow() {
		return passwordPolicyRepo.findFirstByDeletedFalseOrderByIdAsc()
			.orElseThrow(() -> new com.bd.erecruitment.exception.NotFoundException("Password policy not found"));
	}

	private PasswordPolicy getActivePolicy() {
		PasswordPolicy cached = cachedPolicy;
		if (cached != null) return cached;
		PasswordPolicy loaded = passwordPolicyRepo.findFirstByDeletedFalseOrderByIdAsc().orElseGet(this::defaultPolicy);
		cachedPolicy = loaded;
		return loaded;
	}

	private PasswordPolicy defaultPolicy() {
		PasswordPolicyData.PolicyDef def = PasswordPolicyData.getDefault();
		return new PasswordPolicy()
			.setMinLength(def.minLength())
			.setMaxLength(def.maxLength())
			.setRequireUppercase(def.requireUppercase())
			.setRequireLowercase(def.requireLowercase())
			.setRequireDigit(def.requireDigit())
			.setRequireSpecialChar(def.requireSpecialChar())
			.setDisallowUserInfoInPassword(def.disallowUserInfoInPassword());
	}
}
