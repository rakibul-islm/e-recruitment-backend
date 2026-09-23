package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.CompanyType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyTypeReqDto extends BaseRequestDTO<CompanyType> {

	private String name;

	@JsonIgnore
	@Override
	public CompanyType getBean() {
		CompanyType companyType = new CompanyType();
		ModelMapperUtils.MAPPER.map(this, companyType);
		return companyType;
	}
}
