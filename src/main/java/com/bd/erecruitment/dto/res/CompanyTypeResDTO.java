package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.CompanyType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyTypeResDTO extends BaseResponseDTO<CompanyType> {

	public CompanyTypeResDTO(CompanyType companyType) {
		new ModelMapper().map(companyType, this);
	}

	private String name;
}
