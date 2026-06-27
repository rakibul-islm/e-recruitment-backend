package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.Permission;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.modelmapper.ModelMapper;

@Data
@SuperBuilder
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
		new ModelMapper().map(this, p);
		return p;
	}
}
