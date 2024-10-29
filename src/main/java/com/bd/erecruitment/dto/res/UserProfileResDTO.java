package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResDTO{

	public UserProfileResDTO(User user){
		new ModelMapper().map(user, this);
	}

	private String fullName;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private String imageBase64;

	private String roles;
	
}
