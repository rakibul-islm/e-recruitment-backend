package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.modelmapper.ModelMapper;

import java.util.Date;
import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
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

	private Set<Long> roleIds;
	private Set<Long> userGroupIds;

	@JsonIgnore
	@Override
	public User getBean() {
		User u = new User();
		new ModelMapper().map(this, u);
		return u;
	}
}
