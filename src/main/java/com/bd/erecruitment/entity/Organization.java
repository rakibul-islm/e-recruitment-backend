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
// entity and column names deliberately keep "company" so the existing tables, columns and id sequence stay valid
@Entity(name = "Company")
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "COMPANY")
@EqualsAndHashCode(callSuper = true)
public class Organization extends SequenceIdGenerator {

	@Column(nullable = false, length = 200)
	private String name;

	@Column(name = "logo_file_id")
	private Long logoFileId;

	private String website;
	@Column(name = "industry")
	private String sector;
	private String phone;
	private String email;

	@Column(length = 2000)
	private String description;

	private String address;
	private String size;
}
