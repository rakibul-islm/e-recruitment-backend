package com.bd.erecruitment.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.User;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ServiceRepository;
import com.bd.erecruitment.repository.UserRepo;
import com.bd.erecruitment.specification.GenericSpecification;
import com.bd.erecruitment.util.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public abstract class AbstractBaseService<E extends BaseEntity> extends CommonFunctionsImpl {

	@Autowired private UserRepo userRepo;
	protected final ServiceRepository<E> repository;
	protected ModelMapper modelMapper;

	protected AbstractBaseService(ServiceRepository<E> repository) {
		this.repository = repository;
		modelMapper = new ModelMapper();
		modelMapper.getConfiguration().setAmbiguityIgnored(true);
	}

	protected Date getDefaultExpiryDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.YEAR, 50);
		return cal.getTime();
	}

	protected boolean hasAuthority(String authority) {
		MyUserDetail user = getLoggedInUserDetails();
		if (user == null) return false;
		return user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority));
	}

	protected E findByIdOrThrow(Long id, String notFoundMessage) {
		return repository.findByIdAndDeleted(id, false)
				.orElseThrow(() -> new com.bd.erecruitment.exception.NotFoundException(notFoundMessage));
	}

	protected <R> Response<R> genericFilter(Map<String, String> filters, Pageable pageable, Boolean isPageable, Class<R> responseClass) {
		Specification<E> spec = GenericSpecification.build(filters);
		if (Boolean.TRUE.equals(isPageable)) {
			Page<E> page = repository.findAll(spec, pageable);
			return getSuccessResponse(page.hasContent() ? "Found" : "No data found", page.map(e -> modelMapper.map(e, responseClass)));
		}
		List<E> list = repository.findAll(spec);
		List<R> result = list.stream().map(e -> modelMapper.map(e, responseClass)).toList();
		return getSuccessResponse(result.isEmpty() ? "No data found" : "Found", result);
	}

	protected List<E> createAllEntity(List<E> entities) {
		String actor = getLoggedInUserDetails().getUsername();
		Date now = new Date();
		for (E entity : entities) {
			entity.setCreatedBy(actor);
			entity.setCreatedOn(now);
			entity.setUpdatedBy(actor);
			entity.setUpdatedOn(now);
			entity.setDeleted(false);
		}
		return repository.saveAll(entities);
	}

	protected E createEntity(E entity) {
		String actor = getLoggedInUserDetails().getUsername();
		Date now = new Date();
		entity.setCreatedBy(actor);
		entity.setCreatedOn(now);
		entity.setUpdatedBy(actor);
		entity.setUpdatedOn(now);
		entity.setDeleted(false);
		return repository.save(entity);
	}

	protected E createNormalUser(E entity) {
		Date now = new Date();
		entity.setCreatedBy("signup");
		entity.setCreatedOn(now);
		entity.setUpdatedBy("signup");
		entity.setUpdatedOn(now);
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

	protected void removeEntity(E entity) {
		entity.setUpdatedBy(getLoggedInUserDetails().getUsername());
		entity.setUpdatedOn(new Date());
		entity.setDeleted(true);
		repository.save(entity);
	}

	protected MyUserDetail getLoggedInUserDetails() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return null;
		Object principal = auth.getPrincipal();
		return principal instanceof MyUserDetail mud ? mud : null;
	}

	protected User getLoggedInUser() {
		Optional<User> userOp = userRepo.findByIdAndDeleted(getLoggedInUserDetails().getId(), false);
		return userOp.orElse(null);
	}
}
