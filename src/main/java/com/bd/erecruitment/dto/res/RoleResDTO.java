package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Role;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleResDTO extends BaseResponseDTO<Role> {

	private String name;
	private String code;
	private String description;
	private Set<PermissionResDTO> permissions;

	public RoleResDTO(Role role) {
		ModelMapperUtils.MAPPER.map(role, this);
		if (role.getPermissions() != null)
			this.permissions = role.getPermissions().stream()
				.map(PermissionResDTO::new)
				.collect(Collectors.toSet());
	}
}
