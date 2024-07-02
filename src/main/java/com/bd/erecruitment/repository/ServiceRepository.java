package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.BaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * @author Md Rakibul Islam
 */

@NoRepositoryBean
public interface ServiceRepository<E extends BaseEntity> extends JpaRepository<E, Long> {

	Optional<E> findByIdAndDeleted(Long id, boolean deleted);

	List<E> findAllByDeleted(boolean deleted);
	
	List<E> findAllByDeleted(boolean deleted, Sort sort);

	Page<E> findAllByDeleted(boolean deleted, Pageable pageable);

	Page<E> findAllByDeletedOrderByIdDesc(boolean deleted, Pageable pageable);

	List<E> findAllByIdInAndDeleted(List<Long> ids, boolean deleted);
}
