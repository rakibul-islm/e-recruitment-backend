package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

// Lookup list for Company.industry - stored as a managed, database-backed option list (rather
// than free text) so the dropdown can offer existing values and grow as recruiters add new ones.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "COMPANY_TYPE")
@EqualsAndHashCode(callSuper = true)
public class CompanyType extends SequenceIdGenerator {

	@Column(nullable = false, length = 100)
	private String name;
}
