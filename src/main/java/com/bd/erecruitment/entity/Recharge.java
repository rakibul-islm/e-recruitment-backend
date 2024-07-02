package com.bd.erecruitment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Entity
@Table(name = "RECHARGE")
@EqualsAndHashCode(callSuper = true)
public class Recharge extends SequenceIdGenerator{

	private Long amountId;
	@Temporal(TemporalType.TIMESTAMP)
	private Date rechargeDate;
	private String meterNo;
	private BigDecimal currentAmount;
	private String status;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="recharge_vendor_id", nullable=false)
	private RechargeVendor rechargeVendor;

}
