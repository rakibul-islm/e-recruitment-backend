package com.bd.erecruitment.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "ISSUE_DETAILS")
@EqualsAndHashCode(callSuper = true)
public class IssueDetails extends SequenceIdGenerator{

	@Column(length = 9999)
	private String details;
	private String status;
	private String filename;
    private String filetype;
    @Lob
    @Column(name = "filedata")
    private byte[] fileData;
    
    @JsonManagedReference
	@OneToMany(mappedBy = "issueDetails", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@Where(clause = "deleted = false")
	@OrderBy("id asc")
	private List<IssueFeedback> issueFeedback;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="issue_category_id", nullable=false)
	private IssueCategory issueCategory;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="user_account_id", nullable=false)
	private User user;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="admin_id", nullable=false)
	private User admin;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="snd_id", nullable=false)
	private Snd snd;

}
