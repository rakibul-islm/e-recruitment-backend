package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "COMPANY")
@EqualsAndHashCode(callSuper = true)
public class Company extends SequenceIdGenerator {

	@Column(nullable = false, length = 200)
	private String name;

	// References StoredFile.id - no JPA relation, same convention as User.userGroupId.
	@Column(name = "logo_file_id")
	private Long logoFileId;

	private String website;
	private String industry;
	private String phone;
	private String email;

	@Column(length = 2000)
	private String description;

	private String address;
	private String size;
}
