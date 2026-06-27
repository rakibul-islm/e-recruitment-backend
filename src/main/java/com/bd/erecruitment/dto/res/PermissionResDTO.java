package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.Permission;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.modelmapper.ModelMapper;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PermissionResDTO extends BaseResponseDTO<Permission> {

	private String name;
	private String authority;
	private String routeName;
	private String description;
	private String module;

	public PermissionResDTO(Permission p) {
		new ModelMapper().map(p, this);
	}
}
