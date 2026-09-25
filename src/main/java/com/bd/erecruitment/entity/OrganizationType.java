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
@Entity(name = "CompanyType")
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "COMPANY_TYPE")
@EqualsAndHashCode(callSuper = true)
public class OrganizationType extends SequenceIdGenerator {

	@Column(nullable = false, length = 100)
	private String name;
}
