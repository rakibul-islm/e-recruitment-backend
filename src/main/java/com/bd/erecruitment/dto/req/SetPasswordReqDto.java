package com.bd.erecruitment.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SetPasswordReqDto {

	private String token;
	private String newPassword;
	private String confirmPassword;
}
