package com.bd.erecruitment.model;

import com.bd.erecruitment.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class MyUserDetail implements UserDetails {

	private static final long serialVersionUID = -8989844695157859881L;

	private Long id;
	private Long admin_id;
	private Long snd_id;
	private String username;
	private String password;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean superadmin;
	private boolean systemadmin;
	private boolean snduser;
	private boolean normaluser;
	private String roles;
	private List<GrantedAuthority> authorities;
	private boolean enabled;
	private boolean locked;
	private Date expiryDate;

	public MyUserDetail(User user){
		this.id = user.getId();
		this.admin_id = user.getAdmin_id();
		this.snd_id = user.getSnd_id();
		this.username = user.getUsername();
		this.password = user.getPassword();
		this.email = user.getEmail();
		this.address = user.getAddress();
		this.phone = user.getPhone();
		this.mobile = user.getMobile();
		this.superadmin = user.isSuperadmin();
		this.systemadmin = user.isSystemadmin();
		this.snduser = user.isSnduser();
		this.normaluser = user.isNormaluser();
		this.roles = user.getRoles();
		this.authorities = Arrays.stream(roles.split(","))
									.map(SimpleGrantedAuthority::new)
									.collect(Collectors.toList());
		this.enabled = user.isActive();
		this.locked = user.isLocked();
		this.expiryDate = user.getExpiryDate();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public String getUsername() {
		return this.username;
	}

	public String getEmail() {
		return this.email;
	}

	@Override
	public boolean isAccountNonExpired() {
		return new Date().before(this.expiryDate);
	}

	@Override
	public boolean isAccountNonLocked() {
		return !this.locked;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

}
