package com.bd.erecruitment.repository;

import com.bd.erecruitment.entity.GeneratedCv;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GeneratedCvRepo extends ServiceRepository<GeneratedCv> {

	List<GeneratedCv> findAllByCandidateCvIdAndDeletedOrderByGeneratedOnDesc(Long candidateCvId, boolean deleted);
}
