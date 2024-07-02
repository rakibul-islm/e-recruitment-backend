package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;

@MappedSuperclass
@Data
@EqualsAndHashCode(callSuper = true)
public class ManualIdGenerator extends BaseEntity {

	@Id
	@Basic(optional = false)
	private Long id;
}
