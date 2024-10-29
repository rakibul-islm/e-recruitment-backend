package com.bd.erecruitment.service.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ServiceRepository;
import com.bd.erecruitment.repository.UserRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class AbstractBaseService<E extends BaseEntity, R, P> extends CommonFunctionsImpl{

	protected static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	@Autowired protected ApplicationContext appContext;
	@Autowired private UserRepo userRepo;
	private final ServiceRepository<E> repository;
	private ModelMapper modelMapper;

	protected AbstractBaseService(ServiceRepository<E> repository) {
		this.repository = repository;
		modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setAmbiguityIgnored(true);
	}

	protected List<E> createAllEntity(List<E> entities) {
		for(E entity : entities) {
			entity.setCreatedBy(getLoggedInUserDetails().getUsername());
			entity.setCreatedOn(new Date());
			entity.setUpdatedBy(getLoggedInUserDetails().getUsername());
			entity.setUpdatedOn(new Date());
			entity.setDeleted(false);
		}
		return repository.saveAll(entities);
	}

	protected E createEntity(E entity) {
		entity.setCreatedBy(getLoggedInUserDetails().getUsername());
		entity.setCreatedOn(new Date());
		entity.setUpdatedBy(getLoggedInUserDetails().getUsername());
		entity.setUpdatedOn(new Date());
		entity.setDeleted(false);
		return repository.save(entity);
	}
	
	protected E createNormalUser(E entity) {
		entity.setCreatedBy("signup");
		entity.setCreatedOn(new Date());
		entity.setUpdatedBy("signup");
		entity.setUpdatedOn(new Date());
		entity.setDeleted(false);
		return repository.save(entity);
	}

	protected E updateEntity(E entity) {
		entity.setUpdatedBy(getLoggedInUserDetails().getUsername());
		entity.setUpdatedOn(new Date());
		entity.setDeleted(false);
		return repository.save(entity);
	}

	protected void deleteEntity(E entity) {
		repository.delete(entity);
	}

	protected E removeEntityById(E entity) {
		entity.setUpdatedBy(getLoggedInUserDetails().getUsername());
		entity.setUpdatedOn(new Date());
		entity.setDeleted(true);
		return repository.save(entity);
	}

	protected MyUserDetail getLoggedInUserDetails() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth == null || !auth.isAuthenticated()) return null;

		Object principal = auth.getPrincipal();
		if(principal instanceof MyUserDetail) {
			return (MyUserDetail) principal;
		}
		return null;
	}

	protected User getLoggedInUser() {
		Optional<User> userOp = userRepo.findByIdAndDeleted(getLoggedInUserDetails().getId(), false);
		return userOp.isPresent() ? userOp.get() : null;
	}
}
