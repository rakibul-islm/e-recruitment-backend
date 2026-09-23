package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserRoleResDTO extends BaseResponseDTO<Role> {

	private String name;
	private String code;
	private String description;

	public UserRoleResDTO(Role role) {
		ModelMapperUtils.MAPPER.map(role, this);
	}
}
