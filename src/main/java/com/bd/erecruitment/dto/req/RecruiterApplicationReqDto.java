package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.RecruiterApplication;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecruiterApplicationReqDto extends BaseRequestDTO<RecruiterApplication> {

	private String fullName;
	private String email;
	private String phone;
	private String companyName;
	private String companyWebsite;
	private String companyIndustry;
	private String companySize;
	private String companyAddress;
	private String companyPhone;
	private String companyEmail;
	private String companyDescription;
	private String jobTitle;
	private String message;

	@JsonIgnore
	@Override
	public RecruiterApplication getBean() {
		RecruiterApplication application = new RecruiterApplication();
		new ModelMapper().map(this, application);
		return application;
	}
}
