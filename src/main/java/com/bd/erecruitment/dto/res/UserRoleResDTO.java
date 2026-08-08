package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserRoleResDTO extends BaseResponseDTO<Role> {

	private String name;
	private String code;
	private String description;

	public UserRoleResDTO(Role role) {
		new ModelMapper().map(role, this);
	}
}
