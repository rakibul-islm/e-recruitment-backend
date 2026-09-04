package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.Offer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OfferResDTO extends BaseResponseDTO<Offer> {

	public OfferResDTO(Offer offer) {
		new ModelMapper().map(offer, this);
	}

	private Long applicationId;
	private String position;
	private String salaryOffered;
	private Date startDate;
	private Date expiryDate;
	private String status;
	private Long offerLetterFileId;
	private String notes;
	private Date respondedOn;

	// Denormalized, populated by OfferServiceImpl.
	private String jobTitle;
	private String candidateName;
}
