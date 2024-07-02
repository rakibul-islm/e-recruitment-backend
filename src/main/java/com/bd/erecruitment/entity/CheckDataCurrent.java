package com.bd.erecruitment.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "CHECK_DATA_CURRENT")
@EqualsAndHashCode(callSuper = true)
public class CheckDataCurrent extends SequenceIdGenerator{

	private BigDecimal value;
	@Temporal(TemporalType.TIMESTAMP)
	private Date requestDateTime;
	private String meterNo;
	private Long itemId;

	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="user_account_id", nullable=false)
	private User user;
}
