package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Company;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyResDTO extends BaseResponseDTO<Company> {

	public CompanyResDTO(Company company) {
		ModelMapperUtils.MAPPER.map(company, this);
	}

	private String name;
	private Long logoFileId;
	private String website;
	private String industry;
	private String phone;
	private String email;
	private String description;
	private String address;
	private String size;
}
