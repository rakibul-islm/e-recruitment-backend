package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.Offer;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferRepo extends ServiceRepository<Offer> {

	List<Offer> findAllByApplicationIdAndDeletedOrderByIdDesc(Long applicationId, boolean deleted);

	Optional<Offer> findFirstByApplicationIdAndDeletedOrderByIdDesc(Long applicationId, boolean deleted);
}
