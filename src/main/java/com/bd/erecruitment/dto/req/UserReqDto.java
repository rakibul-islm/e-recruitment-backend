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
	private Long admin_id;
	private Long snd_id;
	private String imageBase64;

	private boolean superadmin;
	private boolean systemadmin;
	private boolean snduser;
	private boolean normaluser;

	@JsonIgnore
	@Override
	public User getBean() {
		User u = new User();
		BeanUtils.copyProperties(this, u);
		return u;
	}

}
