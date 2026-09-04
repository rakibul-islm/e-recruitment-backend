package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.Date;

// status: DRAFT, SENT, ACCEPTED, DECLINED, EXPIRED, WITHDRAWN - plain String, same convention as
// Application.status. Offer letter is a PDF generated via the same HTML->PDF pipeline CvGenerationService
// uses (see OfferLetterGenerationService), stored via StorageService.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "OFFER")
@EqualsAndHashCode(callSuper = true)
public class Offer extends SequenceIdGenerator {

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	private String position;
	private String salaryOffered;

	@Temporal(TemporalType.DATE)
	private Date startDate;

	@Temporal(TemporalType.DATE)
	private Date expiryDate;

	@Column(nullable = false, length = 20)
	private String status;

	// References StoredFile.id - no JPA relation, same convention as User.userGroupId.
	@Column(name = "offer_letter_file_id")
	private Long offerLetterFileId;

	@Column(length = 2000)
	private String notes;

	@Temporal(TemporalType.TIMESTAMP)
	private Date respondedOn;
}
