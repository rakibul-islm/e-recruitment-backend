package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.Company;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CompanyReqDto extends BaseRequestDTO<Company> {

	private String name;
	private Long logoFileId;
	private String website;
	private String industry;
	private String phone;
	private String email;
	private String description;
	private String address;
	private String size;

	@JsonIgnore
	@Override
	public Company getBean() {
		Company c = new Company();
		new ModelMapper().map(this, c);
		return c;
	}
}
