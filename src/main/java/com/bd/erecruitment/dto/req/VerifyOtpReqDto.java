package com.bd.erecruitment.dto.req;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VerifyOtpReqDto {

	private String email;
	private String otp;
}
