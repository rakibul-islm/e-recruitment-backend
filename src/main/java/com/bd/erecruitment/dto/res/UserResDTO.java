package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.modelmapper.ModelMapper;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserResDTO extends BaseResponseDTO<User> {

	private String fullName;
	private String username;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean active;
	private boolean locked;
	private Date expiryDate;
	private Set<RoleResDTO> roles;

	public UserResDTO(User user) {
		new ModelMapper().map(user, this);
		if (user.getRoles() != null)
			this.roles = user.getRoles().stream()
				.map(RoleResDTO::new)
				.collect(Collectors.toSet());
	}
}
