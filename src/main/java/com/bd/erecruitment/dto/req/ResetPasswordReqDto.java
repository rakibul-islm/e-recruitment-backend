package com.bd.erecruitment.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResetPasswordReqDto {

	private String email;
	private String otp;
	private String newPassword;
	private String confirmPassword;
}
