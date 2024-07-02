package com.bd.erecruitment.entity;

import jakarta.persistence.*;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.Date;

@MappedSuperclass
@Data
public class BaseEntity {

	@Column(name = "deleted", nullable = false)
	private boolean deleted;

	@NotNull
	@Column(name = "created_by", nullable = false, length = 20)
	private String createdBy;

	@NotNull
	@Temporal(TemporalType.TIMESTAMP)
	private Date createdOn;

	@NotNull
	@Column(name = "updated_by", nullable = false, length = 20)
	private String updatedBy;

	@Temporal(TemporalType.TIMESTAMP)
	private Date updatedOn;

}
