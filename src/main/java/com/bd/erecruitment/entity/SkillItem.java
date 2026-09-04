package com.bd.erecruitment.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
public class SkillItem {

	private String name;

	// Free text (e.g. "Beginner"/"Intermediate"/"Expert") - kept simple, no enum.
	private String level;
}
