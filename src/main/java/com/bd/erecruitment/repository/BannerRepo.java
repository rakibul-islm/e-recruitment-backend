package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Banner;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BannerRepo extends ServiceRepository<Banner> {

	Optional<Banner> findFirstByActiveTrueAndDeletedFalse();

	boolean existsByFileIdAndDeletedFalse(Long fileId);

	long countByDeletedFalse();

	/** At most one banner is shown on the dashboard; activating one retires all others. */
	@Modifying
	@Query("update Banner b set b.active = false where b.active = true and b.deleted = false")
	int deactivateAll();
}
