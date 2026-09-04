package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.Date;

@Data
public class CreateOfferReqDto {

	private Long applicationId;
	private String position;
	private String salaryOffered;
	private Date startDate;
	private Date expiryDate;
	private String notes;
}
