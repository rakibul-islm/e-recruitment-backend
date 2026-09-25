package com.bd.erecruitment.entity;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
public class SkillItem {

	private String name;

	private String level;
}
