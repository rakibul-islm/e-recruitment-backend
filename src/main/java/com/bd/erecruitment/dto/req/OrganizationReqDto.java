package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Organization;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationReqDto extends BaseRequestDTO<Organization> {

	private String name;
	private Long logoFileId;
	private String website;
	private String organizationType;
	private String phone;
	private String email;
	private String description;
	private String address;
	private String size;

	@JsonIgnore
	@Override
	public Organization getBean() {
		Organization c = new Organization();
		ModelMapperUtils.MAPPER.map(this, c);
		return c;
	}
}
