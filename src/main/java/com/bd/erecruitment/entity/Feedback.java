package com.bd.erecruitment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

@Data
@Entity
@Table(name = "FEEDBACK")
@EqualsAndHashCode(callSuper = true)
public class Feedback extends SequenceIdGenerator{
	
	private String details;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="user_account_id", nullable=false)
	private User user;
}
