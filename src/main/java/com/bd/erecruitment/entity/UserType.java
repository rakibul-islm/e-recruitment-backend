package com.bd.erecruitment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "USER_TYPE")
@EqualsAndHashCode(callSuper = true)
public class UserType extends SequenceIdGenerator{

	private String type;
	private String details;
}
