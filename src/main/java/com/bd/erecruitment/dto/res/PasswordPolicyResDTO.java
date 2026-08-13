package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.PasswordPolicy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

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
		new ModelMapper().map(policy, this);
	}
}
