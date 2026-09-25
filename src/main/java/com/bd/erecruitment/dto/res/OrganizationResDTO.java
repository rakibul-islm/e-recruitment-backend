package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Organization;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationResDTO extends BaseResponseDTO<Organization> {

	public OrganizationResDTO(Organization organization) {
		ModelMapperUtils.MAPPER.map(organization, this);
	}

	private String name;
	private Long logoFileId;
	private String website;
	private String sector;
	private String phone;
	private String email;
	private String description;
	private String address;
	private String size;
}
