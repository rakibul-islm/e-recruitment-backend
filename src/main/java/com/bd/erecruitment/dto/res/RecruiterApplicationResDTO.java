package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.RecruiterApplication;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecruiterApplicationResDTO extends BaseResponseDTO<RecruiterApplication> {

	public RecruiterApplicationResDTO(RecruiterApplication application) {
		new ModelMapper().map(application, this);
	}

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
	private String status;
	private String reviewNote;
	private Date createdOn;
	private String updatedBy;
	private Date updatedOn;
}
