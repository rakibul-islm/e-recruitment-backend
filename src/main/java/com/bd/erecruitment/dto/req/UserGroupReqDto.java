package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.UserGroup;
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
public class UserGroupReqDto extends BaseRequestDTO<UserGroup> {

	private String name;
	private String description;
	private Set<Long> roleIds;

	@JsonIgnore
	@Override
	public UserGroup getBean() {
		UserGroup g = new UserGroup();
		new ModelMapper().map(this, g);
		return g;
	}
}
