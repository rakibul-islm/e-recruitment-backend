package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.RecruiterApplication;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RecruiterApplicationResDTO extends BaseResponseDTO<RecruiterApplication> {

	public RecruiterApplicationResDTO(RecruiterApplication application) {
		ModelMapperUtils.MAPPER.map(application, this);
	}

	private String fullName;
	private String email;
	private String phone;
	private String organizationName;
	private String organizationWebsite;
	private String organizationSector;
	private String organizationSize;
	private String organizationAddress;
	private String organizationPhone;
	private String organizationEmail;
	private String organizationDescription;
	private String jobTitle;
	private String message;
	private String status;
	private String reviewNote;
	private Date createdOn;
	private String updatedBy;
	private Date updatedOn;
}
