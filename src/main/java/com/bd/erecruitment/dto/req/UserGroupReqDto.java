package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.UserGroup;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserGroupReqDto extends BaseRequestDTO<UserGroup> {

	private String name;
	private String description;
	private Set<Long> roleIds;

	@JsonIgnore
	@Override
	public UserGroup getBean() {
		UserGroup g = new UserGroup();
		ModelMapperUtils.MAPPER.map(this, g);
		return g;
	}
}
