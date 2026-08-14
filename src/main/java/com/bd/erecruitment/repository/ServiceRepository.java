package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface ServiceRepository<E extends BaseEntity> extends JpaRepository<E, Long>, JpaSpecificationExecutor<E> {

	Optional<E> findByIdAndDeleted(Long id, boolean deleted);

	List<E> findAllByIdInAndDeleted(List<Long> ids, boolean deleted);
}
