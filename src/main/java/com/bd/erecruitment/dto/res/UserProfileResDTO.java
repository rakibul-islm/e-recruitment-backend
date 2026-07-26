package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResDTO{

	public UserProfileResDTO(User user){
		new ModelMapper().map(user, this);
		if (user.getRoles() != null)
			this.roles = user.getRoles().stream()
				.map(UserRoleResDTO::new)
				.collect(Collectors.toSet());
	}

	private Long id;
	private String fullName;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private String imageBase64;

	private Set<UserRoleResDTO> roles;

}
