package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.modelmapper.ModelMapper;

import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleReqDto extends BaseRequestDTO<Role> {

	private String name;
	private String code;
	private String description;
	private Set<Long> permissionIds;

	@JsonIgnore
	@Override
	public Role getBean() {
		Role r = new Role();
		new ModelMapper().map(this, r);
		return r;
	}
}
