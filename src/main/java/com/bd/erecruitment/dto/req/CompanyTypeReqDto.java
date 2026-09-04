package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.CompanyType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyTypeReqDto extends BaseRequestDTO<CompanyType> {

	private String name;

	@JsonIgnore
	@Override
	public CompanyType getBean() {
		CompanyType companyType = new CompanyType();
		new ModelMapper().map(this, companyType);
		return companyType;
	}
}
