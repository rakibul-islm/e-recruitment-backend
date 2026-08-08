package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import java.util.Date;

@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@Accessors(chain = true)
public class BaseEntity {

	@Column(name = "deleted", nullable = false)
	private boolean deleted;

	@NotNull
	@Column(name = "created_by", nullable = false, length = 50)
	private String createdBy;

	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdOn;

	@NotNull
	@Column(name = "updated_by", nullable = false, length = 50)
	private String updatedBy;

	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedOn;

	@Column(name = "created_terminal", length = 100)
	private String createdTerminal;

	@Column(name = "updated_terminal", length = 100)
	private String updatedTerminal;

}
