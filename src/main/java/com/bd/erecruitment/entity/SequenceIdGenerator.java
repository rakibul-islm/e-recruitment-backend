package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public class SequenceIdGenerator extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_gen")
	@SequenceGenerator(name = "id_gen", allocationSize = 1)
	private Long id;
}
