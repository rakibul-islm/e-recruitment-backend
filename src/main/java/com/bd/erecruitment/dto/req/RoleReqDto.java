package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
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
		ModelMapperUtils.MAPPER.map(this, r);
		return r;
	}
}
