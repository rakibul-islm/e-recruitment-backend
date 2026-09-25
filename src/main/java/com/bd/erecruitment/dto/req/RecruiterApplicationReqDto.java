package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.RecruiterApplication;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecruiterApplicationReqDto extends BaseRequestDTO<RecruiterApplication> {

	private String fullName;
	private String email;
	private String phone;
	private String organizationName;
	private String organizationWebsite;
	private String organizationType;
	private String organizationSize;
	private String organizationAddress;
	private String organizationPhone;
	private String organizationEmail;
	private String organizationDescription;
	private String jobTitle;
	private String message;

	@JsonIgnore
	@Override
	public RecruiterApplication getBean() {
		RecruiterApplication application = new RecruiterApplication();
		ModelMapperUtils.MAPPER.map(this, application);
		return application;
	}
}
