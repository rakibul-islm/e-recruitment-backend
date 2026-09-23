package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.CompanyType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyTypeResDTO extends BaseResponseDTO<CompanyType> {

	public CompanyTypeResDTO(CompanyType companyType) {
		ModelMapperUtils.MAPPER.map(companyType, this);
	}

	private String name;
}
