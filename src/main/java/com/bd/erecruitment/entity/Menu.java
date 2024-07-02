package com.bd.erecruitment.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Entity
@Table(name = "MENU")
@EqualsAndHashCode(callSuper = true)
public class Menu extends SequenceIdGenerator{

	private String mName;
	@NotNull
	private Long mSequence;
	private int parentsId;
	private String url;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@Where(clause = "deleted = false")
	private List<PermissionMenu> permissionMenu;

}
