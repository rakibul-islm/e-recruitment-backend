package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.Permission;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PermissionReqDto extends BaseRequestDTO<Permission> {

	private String name;
	private String authority;
	private String routeName;
	private String description;
	private String module;

	@JsonIgnore
	@Override
	public Permission getBean() {
		Permission p = new Permission();
		ModelMapperUtils.MAPPER.map(this, p);
		return p;
	}
}
