package com.bd.erecruitment.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonBackReference;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "ISSUE_FEEDBACK")
@EqualsAndHashCode(callSuper = true)
public class IssueFeedback extends SequenceIdGenerator{

	@Column(length = 9999)
	private String details;
	private String filename;
    private String filetype;
    @Lob
    @Column(name = "filedata")
    private byte[] fileData;
	
    @JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="issue_details_id", nullable=false)
	private IssueDetails issueDetails;

}
