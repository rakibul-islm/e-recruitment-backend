package com.bd.erecruitment.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Embeddable
@NoArgsConstructor
public class CertificationItem {

	private String name;
	private String issuer;

	@Temporal(TemporalType.DATE)
	private Date date;

	private String credentialUrl;
}
