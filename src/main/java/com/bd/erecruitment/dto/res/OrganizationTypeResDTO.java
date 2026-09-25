package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.OrganizationType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrganizationTypeResDTO extends BaseResponseDTO<OrganizationType> {

	public OrganizationTypeResDTO(OrganizationType organizationType) {
		ModelMapperUtils.MAPPER.map(organizationType, this);
	}

	private String name;
}
