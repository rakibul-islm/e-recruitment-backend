package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserSignupReqDto extends BaseRequestDTO<User> {

	private String fullName;
	private String username;
	private String password;
	private String email;
	private String address;
	private String mobile;

	@JsonIgnore
	@Override
	public User getBean() {
		User u = new User();
		BeanUtils.copyProperties(this, u);
		return u;
	}

}
