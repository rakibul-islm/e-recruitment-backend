package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.PasswordPolicy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PasswordPolicyResDTO extends BaseResponseDTO<PasswordPolicy> {

	private int minLength;
	private int maxLength;
	private boolean requireUppercase;
	private boolean requireLowercase;
	private boolean requireDigit;
	private boolean requireSpecialChar;
	private boolean disallowUserInfoInPassword;

	public PasswordPolicyResDTO(PasswordPolicy policy) {
		ModelMapperUtils.MAPPER.map(policy, this);
	}
}
