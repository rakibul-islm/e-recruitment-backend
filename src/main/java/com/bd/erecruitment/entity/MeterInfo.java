package com.bd.erecruitment.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "METERINFO")
@EqualsAndHashCode(callSuper = true)
public class MeterInfo extends SequenceIdGenerator{

	@Column(name = "meterNo", nullable = false)
	private String meterNo;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="user_account_id", nullable=false)
	private User user;
}
