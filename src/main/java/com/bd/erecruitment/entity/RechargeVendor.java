package com.bd.erecruitment.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Where;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Table(name = "RECHARGE_VENDOR")
@EqualsAndHashCode(callSuper = true)
public class RechargeVendor extends SequenceIdGenerator{

	private String vendorName;
	private String vendorContactPerson;
	
	@JsonManagedReference
	@OneToMany(mappedBy = "rechargeVendor", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@Fetch(value = FetchMode.SUBSELECT)
	@Where(clause = "deleted = false")
	private List<Recharge> recharge;

}
