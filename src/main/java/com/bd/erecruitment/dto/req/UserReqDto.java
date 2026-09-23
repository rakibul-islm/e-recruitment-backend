package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.util.ModelMapperUtils;

import com.bd.erecruitment.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Set;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserReqDto extends BaseRequestDTO<User> {

	private String fullName;
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
	private Long userGroupId;
	private Long companyId;

	@JsonIgnore
	@Override
	public User getBean() {
		User u = new User();
		ModelMapperUtils.MAPPER.map(this, u);
		return u;
	}
}
