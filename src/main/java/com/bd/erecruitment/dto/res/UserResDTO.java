package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserResDTO extends BaseResponseDTO<User>{

	public UserResDTO(User user){
		new ModelMapper().map(user, this);
	}

	private String fullName;
	private String username;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean active;
	private boolean locked;
	private Date expiryDate;

	private boolean superAdmin;
	private boolean systemAdmin;
	private boolean recruiterUser;
	private boolean candidateUser;

	private String roles;
	
}
