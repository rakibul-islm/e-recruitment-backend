package com.bd.erecruitment.util;

import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.entity.ManualIdGenerator;
import com.bd.erecruitment.entity.SequenceIdGenerator;
import com.bd.erecruitment.repository.ServiceRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-call memo for "find non-deleted entity by id" lookups, used while turning a list of rows into DTOs.
 * {@link #preload} fetches all the ids a list will need in one query per entity type; {@link #get} then answers from
 * memory (and memoizes any id it had to fetch itself), replacing the one-query-per-row pattern. Not thread-safe:
 * create one per request/operation.
 */
public class EntityLookup {

	private static final int CHUNK_SIZE = 500; // stay under Oracle's 1000-element IN-list limit

	private final Map<ServiceRepository<?>, Map<Long, Optional<?>>> cache = new IdentityHashMap<>();

	public <E extends BaseEntity> EntityLookup preload(ServiceRepository<E> repo, Collection<Long> ids) {
		Map<Long, Optional<?>> memo = cache.computeIfAbsent(repo, r -> new HashMap<>());
		List<Long> missing = ids.stream().filter(Objects::nonNull).distinct().filter(id -> !memo.containsKey(id)).toList();
		for (int from = 0; from < missing.size(); from += CHUNK_SIZE) {
			List<Long> chunk = new ArrayList<>(missing.subList(from, Math.min(from + CHUNK_SIZE, missing.size())));
			for (E entity : repo.findAllByIdInAndDeleted(chunk, false)) memo.put(idOf(entity), Optional.of(entity));
			chunk.forEach(id -> memo.putIfAbsent(id, Optional.empty()));
		}
		return this;
	}

	// The id lives on the two id-generator base classes, not on BaseEntity itself.
	private static Long idOf(BaseEntity entity) {
		if (entity instanceof SequenceIdGenerator s) return s.getId();
		if (entity instanceof ManualIdGenerator m) return m.getId();
		throw new IllegalArgumentException("Unsupported entity type " + entity.getClass().getName());
	}

	@SuppressWarnings("unchecked")
	public <E extends BaseEntity> E get(ServiceRepository<E> repo, Long id) {
		if (id == null) return null;
		Map<Long, Optional<?>> memo = cache.computeIfAbsent(repo, r -> new HashMap<>());
		return (E) memo.computeIfAbsent(id, k -> repo.findByIdAndDeleted(k, false)).orElse(null);
	}
}
