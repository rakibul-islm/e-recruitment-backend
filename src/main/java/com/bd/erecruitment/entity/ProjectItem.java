package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Embeddable
@NoArgsConstructor
public class ProjectItem {

	private String name;

	@Column(length = 2000)
	private String description;

	private String url;
}
