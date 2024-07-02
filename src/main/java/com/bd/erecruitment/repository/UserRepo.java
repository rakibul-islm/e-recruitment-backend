package com.bd.erecruitment.repository;
import com.bd.erecruitment.entity.User;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface UserRepo extends ServiceRepository<User> {
	public User findByUsername(String username);
	public User findByEmail(String email);
}
