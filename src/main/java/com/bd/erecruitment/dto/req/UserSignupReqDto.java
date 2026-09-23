package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserSignupReqDto extends BaseRequestDTO<User> {

	private String fullName;
	private String password;
	private String email;
	private String mobile;

	@JsonIgnore
	@Override
	public User getBean() {
		User u = new User();
		ModelMapperUtils.MAPPER.map(this, u);
		return u;
	}

}
