package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

// A frozen copy of one McqOptionItem, captured onto McqTestAssignmentQuestion at assignment time
// so a later edit to the bank question can never change an in-flight or completed attempt.
@Data
@Embeddable
@NoArgsConstructor
@Accessors(chain = true)
public class McqAssignmentOptionItem {

	@Column(name = "option_key", length = 5)
	private String optionKey;

	@Column(name = "option_text", length = 500)
	private String optionText;

	private boolean correct;

	@Column(name = "display_order")
	private int displayOrder;
}
