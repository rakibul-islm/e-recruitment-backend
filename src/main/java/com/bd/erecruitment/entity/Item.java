package com.bd.erecruitment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "ITEM")
@EqualsAndHashCode(callSuper = true)
public class Item extends SequenceIdGenerator{

	@Column(name = "name", nullable = false)
	private String name;
	private String itemDtails;
	private Long dlmsid;

}
