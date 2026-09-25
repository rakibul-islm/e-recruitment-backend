package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.OrganizationType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationTypeReqDto extends BaseRequestDTO<OrganizationType> {

	private String name;

	@JsonIgnore
	@Override
	public OrganizationType getBean() {
		OrganizationType organizationType = new OrganizationType();
		ModelMapperUtils.MAPPER.map(this, organizationType);
		return organizationType;
	}
}
