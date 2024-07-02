package com.bd.erecruitment.entity;

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
@Table(name = "ISSUE_CATEGORY")
@EqualsAndHashCode(callSuper = true)
public class IssueCategory extends SequenceIdGenerator{

	private String name;
	private boolean requiredDocumnt;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "issueCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@Where(clause = "deleted = false")
	private List<IssueDetails> issueDetails;

}
