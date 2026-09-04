package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// One interview round for an Application. status: SCHEDULED, COMPLETED, CANCELLED - plain String,
// same convention as Application.status/JobCircular.status.
@Data
@Entity
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "INTERVIEW")
@EqualsAndHashCode(callSuper = true)
public class Interview extends SequenceIdGenerator {

	@Column(name = "application_id", nullable = false)
	private Long applicationId;

	@Column(nullable = false, length = 100)
	private String title;

	@Temporal(TemporalType.TIMESTAMP)
	private Date scheduledAt;

	private Integer durationMinutes;

	// ONSITE | PHONE | VIDEO - plain String.
	private String mode;

	// Address for ONSITE, dial-in for PHONE, meeting link for VIDEO.
	@Column(length = 500)
	private String location;

	@Column(nullable = false, length = 20)
	private String status;

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "INTERVIEW_INTERVIEWER", joinColumns = @JoinColumn(name = "interview_id"))
	@Column(name = "user_id")
	private List<Long> interviewerUserIds = new ArrayList<>();

	@Builder.Default
	@ElementCollection
	@CollectionTable(name = "INTERVIEW_FEEDBACK", joinColumns = @JoinColumn(name = "interview_id"))
	private List<InterviewFeedbackItem> feedback = new ArrayList<>();
}
