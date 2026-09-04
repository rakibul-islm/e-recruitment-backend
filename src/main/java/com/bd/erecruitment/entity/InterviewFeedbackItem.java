package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@Embeddable
@NoArgsConstructor
@Accessors(chain = true)
public class InterviewFeedbackItem {

	private Long interviewerUserId;
	private String interviewerName;

	// 1-5, free-form (no @Min/@Max - kept simple, same convention as other embeddables here).
	private Integer rating;

	@Column(length = 2000)
	private String comments;

	@Temporal(TemporalType.TIMESTAMP)
	private Date submittedOn;
}
