package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.PasswordPolicy;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PasswordPolicyReqDto extends BaseRequestDTO<PasswordPolicy> {

	private int minLength;
	private int maxLength;
	private boolean requireUppercase;
	private boolean requireLowercase;
	private boolean requireDigit;
	private boolean requireSpecialChar;
	private boolean disallowUserInfoInPassword;

	@JsonIgnore
	@Override
	public PasswordPolicy getBean() {
		PasswordPolicy p = new PasswordPolicy();
		new ModelMapper().map(this, p);
		return p;
	}
}
