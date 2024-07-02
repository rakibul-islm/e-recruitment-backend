package com.bd.erecruitment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@Table(name = "COMPLAIN_DETAILS")
@EqualsAndHashCode(callSuper = true)
public class ComplainDetails extends SequenceIdGenerator{

	private String detail;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="complain_master_id", nullable=false)
	private ComplainMaster complainMaster;

}
