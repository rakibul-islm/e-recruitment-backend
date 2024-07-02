package com.bd.erecruitment.entity;

import com.asl.issuetrack.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Data
@Entity
@Table(name = "PERMISSION_MENU")
@EqualsAndHashCode(callSuper = true)
public class PermissionMenu extends SequenceIdGenerator{

	@NotNull
	@Enumerated(EnumType.STRING)
	private UserRole role;	
	
	@NotNull
	private Long sequence;
	
	@JsonBackReference
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="menu_id", nullable=false)
	private Menu menu;

}
