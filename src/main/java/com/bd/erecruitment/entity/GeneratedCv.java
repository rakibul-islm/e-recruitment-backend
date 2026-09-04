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

// One row per candidate profile at a time - CandidateProfileServiceImpl.generateCv() soft-deletes
// any prior generation before creating a new one, so regenerating replaces rather than accumulates.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "GENERATED_CV")
@EqualsAndHashCode(callSuper = true)
public class GeneratedCv extends SequenceIdGenerator {

	@Column(name = "candidate_profile_id", nullable = false)
	private Long candidateProfileId;

	@Column(name = "template_key", nullable = false, length = 50)
	private String templateKey;

	// References StoredFile.id - no JPA relation, same convention as User.userGroupId.
	@Column(name = "stored_file_id", nullable = false)
	private Long storedFileId;

	@Temporal(TemporalType.TIMESTAMP)
	private Date generatedOn;
}
