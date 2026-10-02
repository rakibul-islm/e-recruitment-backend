package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Offer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepo extends ServiceRepository<Offer> {

	List<Offer> findAllByApplicationIdAndDeletedOrderByIdDesc(Long applicationId, boolean deleted);

	/** Offers on a candidate's non-deleted applications, in one query (no separate applications lookup). */
	@Query("select o from Offer o where o.deleted = false and o.applicationId in " +
		"(select a.id from Application a where a.candidateUserId = :userId and a.deleted = false) order by o.id desc")
	List<Offer> findAllByCandidateUserId(@Param("userId") Long userId);

	Optional<Offer> findFirstByApplicationIdAndDeletedOrderByIdDesc(Long applicationId, boolean deleted);
}
