package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.modelmapper.ModelMapper;

@Data
@SuperBuilder
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
