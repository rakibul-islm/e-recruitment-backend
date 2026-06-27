package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.UserGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.modelmapper.ModelMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserGroupResDTO extends BaseResponseDTO<UserGroup> {

	private String name;
	private String description;
	private Set<RoleResDTO> roles;

	public UserGroupResDTO(UserGroup group) {
		new ModelMapper().map(group, this);
		if (group.getRoles() != null)
			this.roles = group.getRoles().stream()
				.map(RoleResDTO::new)
				.collect(Collectors.toSet());
	}
}
