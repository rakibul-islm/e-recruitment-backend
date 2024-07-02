package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.beans.BeanUtils;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserReqDto extends BaseRequestDTO<User> {

	private String fullName;
	private String username;
	private String password;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean active;
	private boolean locked;
	private Date expiryDate;
	private String imageBase64;

	private boolean superAdmin;
	private boolean systemAdmin;
	private boolean recruiterUser;
	private boolean candidateUser;

	@JsonIgnore
	@Override
	public User getBean() {
		User u = new User();
		BeanUtils.copyProperties(this, u);
		return u;
	}

}
