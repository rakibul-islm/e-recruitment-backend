package com.bd.erecruitment.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.Entity;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "SND")
@EqualsAndHashCode(callSuper = true)
public class Snd extends SequenceIdGenerator{

	private Long code;
	private String name;
	
}
