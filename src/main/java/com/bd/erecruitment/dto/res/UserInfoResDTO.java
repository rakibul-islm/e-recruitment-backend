package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResDTO extends BaseResponseDTO<User>{

	public UserInfoResDTO(User user){
		new ModelMapper().map(user, this);
	}

	private String fullName;
	private String mobile;
	
}
