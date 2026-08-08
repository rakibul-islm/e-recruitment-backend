package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.entity.UserGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserResDTO extends BaseResponseDTO<User> {

	private String fullName;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean active;
	private boolean locked;
	private Date expiryDate;
	private Set<UserRoleResDTO> roles;
	private UserGroupResDTO userGroup;

	public UserResDTO(User user) {
		new ModelMapper().map(user, this);
		if (user.getRoles() != null)
			this.roles = user.getRoles().stream()
				.map(UserRoleResDTO::new)
				.collect(Collectors.toSet());
		if (user.getUserGroup() != null) {
			UserGroup group = user.getUserGroup();
			this.userGroup = new UserGroupResDTO();
			this.userGroup.setId(group.getId());
			this.userGroup.setName(group.getName());
			this.userGroup.setDescription(group.getDescription());
		}
	}
}
