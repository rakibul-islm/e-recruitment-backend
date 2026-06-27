package com.bd.erecruitment.model;

import com.bd.erecruitment.entity.User;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class MyUserDetail implements UserDetails {

	private static final long serialVersionUID = -8989844695157859881L;

	private Long id;
	private String username;
	private String password;
	private String email;
	private String address;
	private String phone;
	private String mobile;
	private boolean enabled;
	private boolean locked;
	private Date expiryDate;
	private List<GrantedAuthority> authorities;

	public MyUserDetail(User user) {
		this.id = user.getId();
		this.username = user.getUsername();
		this.password = user.getPassword();
		this.email = user.getEmail();
		this.address = user.getAddress();
		this.phone = user.getPhone();
		this.mobile = user.getMobile();
		this.enabled = user.isActive();
		this.locked = user.isLocked();
		this.expiryDate = user.getExpiryDate();

		Set<GrantedAuthority> auths = new HashSet<>();

		// Direct role → permission authorities
		if (user.getRoles() != null)
			user.getRoles().stream()
				.flatMap(r -> r.getPermissions().stream())
				.map(p -> new SimpleGrantedAuthority(p.getAuthority()))
				.forEach(auths::add);

		// UserGroup → role → permission authorities
		if (user.getUserGroups() != null)
			user.getUserGroups().stream()
				.flatMap(g -> g.getRoles().stream())
				.flatMap(r -> r.getPermissions().stream())
				.map(p -> new SimpleGrantedAuthority(p.getAuthority()))
				.forEach(auths::add);

		this.authorities = new ArrayList<>(auths);
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
