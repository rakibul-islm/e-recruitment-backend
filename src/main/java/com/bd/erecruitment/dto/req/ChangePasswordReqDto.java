package com.bd.erecruitment.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChangePasswordReqDto {

	private String oldPassword;
	private String otp;
	private String newPassword;
	private String confirmPassword;
}
