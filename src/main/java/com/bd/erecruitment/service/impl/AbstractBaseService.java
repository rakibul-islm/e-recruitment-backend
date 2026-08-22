package com.bd.erecruitment.service.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.bd.erecruitment.audit.AuditAction;
import com.bd.erecruitment.audit.AuditExempt;
import com.bd.erecruitment.audit.AuditIgnore;
import com.bd.erecruitment.audit.AuditLogWriter;
import com.bd.erecruitment.entity.BaseEntity;
import com.bd.erecruitment.enums.AuditOutcome;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.ServiceRepository;
import com.bd.erecruitment.specification.GenericSpecification;
import com.bd.erecruitment.util.RequestUtils;
import com.bd.erecruitment.util.Response;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;

@Slf4j
public abstract class AbstractBaseService<E extends BaseEntity> extends CommonFunctionsImpl {

	protected final ServiceRepository<E> repository;
	protected ModelMapper modelMapper;

	@Autowired
	private AuditLogWriter auditLogWriter;

	@Autowired
	private ObjectMapper objectMapper;

	// Pre-mutation field snapshots, captured at fetch time (see captureSnapshot) and diffed
	// against the post-save state in audit(). Keyed by object identity (not equals/hashCode,
	// which is id-based and unreliable pre-save) and thread-local since services are singletons
	// shared across requests. Entries are removed both on use and via a transaction-completion
	// synchronization, so a request that throws between fetch and save never leaks an entry.
	// Values are either a String (scalar/to-one field) or a List<String> (entity collection field).
	private final ThreadLocal<Map<Object, Map<String, Object>>> auditSnapshots = ThreadLocal.withInitial(IdentityHashMap::new);

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

	protected E findByIdOrThrow(Long id, String notFoundMessage) {
		E entity = repository.findByIdAndDeleted(id, false)
				.orElseThrow(() -> new com.bd.erecruitment.exception.NotFoundException(notFoundMessage));
		return captureSnapshot(entity);
	}

	/**
	 * Records this entity's current diffable field values so a later update() can diff old vs.
	 * new. Called automatically by findByIdOrThrow; call explicitly for fetches that bypass it
	 * (e.g. PasswordPolicyServiceImpl's own "first non-deleted row" finder). Never allowed to
	 * fail the fetch it's piggybacking on — worst case is a skipped diff, not a broken read.
	 */
	protected E captureSnapshot(E entity) {
		if (entity == null) return null;
		try {
			auditSnapshots.get().put(entity, extractDiffableFields(entity));
		} catch (Exception ex) {
			log.warn("Failed to snapshot {} for audit diffing: {}", entity.getClass().getSimpleName(), ex.getMessage());
		}
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					auditSnapshots.get().remove(entity);
				}
			});
		}
		return entity;
	}

	protected <R> Response<R> genericFilter(Map<String, String> filters, Pageable pageable, Boolean isPageable, Class<R> responseClass) {
		return genericFilter(GenericSpecification.build(filters), pageable, isPageable, responseClass);
	}

	// For services that need to layer extra predicates (e.g. a computed "active" status, or a
	// join to a related entity) on top of - or instead of - the generic filter map.
	protected <R> Response<R> genericFilter(Specification<E> spec, Pageable pageable, Boolean isPageable, Class<R> responseClass) {
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
		String terminal = RequestUtils.getClientTerminal();
		Date now = new Date();
		for (E entity : entities) {
			entity.setCreatedBy(actor);
			entity.setCreatedOn(now);
			entity.setCreatedTerminal(terminal);
			entity.setUpdatedBy(actor);
			entity.setUpdatedOn(now);
			entity.setUpdatedTerminal(terminal);
			entity.setDeleted(false);
		}
		List<E> saved = repository.saveAll(entities);
		saved.forEach(entity -> audit(AuditAction.CREATE, entity));
		return saved;
	}

	protected E createEntity(E entity) {
		String actor = getLoggedInUserDetails().getUsername();
		String terminal = RequestUtils.getClientTerminal();
		Date now = new Date();
		entity.setCreatedBy(actor);
		entity.setCreatedOn(now);
		entity.setCreatedTerminal(terminal);
		entity.setUpdatedBy(actor);
		entity.setUpdatedOn(now);
		entity.setUpdatedTerminal(terminal);
		entity.setDeleted(false);
		E saved = repository.save(entity);
		audit(AuditAction.CREATE, saved);
		return saved;
	}

	protected E createNormalUser(E entity) {
		String terminal = RequestUtils.getClientTerminal();
		Date now = new Date();
		entity.setCreatedBy("signup");
		entity.setCreatedOn(now);
		entity.setCreatedTerminal(terminal);
		entity.setUpdatedBy("signup");
		entity.setUpdatedOn(now);
		entity.setUpdatedTerminal(terminal);
		entity.setDeleted(false);
		return repository.save(entity);
	}

	protected E updateEntity(E entity) {
		entity.setUpdatedBy(getLoggedInUserDetails().getUsername());
		entity.setUpdatedOn(new Date());
		entity.setUpdatedTerminal(RequestUtils.getClientTerminal());
		entity.setDeleted(false);
		E saved = repository.save(entity);
		audit(AuditAction.UPDATE, saved);
		return saved;
	}

	protected void deleteEntity(E entity) {
		// Synchronous, same transaction: an irreversible action can't be fire-and-forget.
		auditSync(AuditAction.HARD_DELETE, entity);
		repository.delete(entity);
	}

	protected void removeEntity(E entity) {
		entity.setUpdatedBy(getLoggedInUserDetails().getUsername());
		entity.setUpdatedOn(new Date());
		entity.setUpdatedTerminal(RequestUtils.getClientTerminal());
		entity.setDeleted(true);
		E saved = repository.save(entity);
		audit(AuditAction.SOFT_DELETE, saved);
	}

	protected MyUserDetail getLoggedInUserDetails() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return null;
		Object principal = auth.getPrincipal();
		return principal instanceof MyUserDetail mud ? mud : null;
	}

	private void audit(String action, E entity) {
		if (isAuditExempt()) return;
		String changedFields = buildChangedFieldsJson(action, entity);
		auditLogWriter.logEntity(action, entity.getClass().getSimpleName(), extractId(entity), AuditOutcome.SUCCESS, changedFields);
	}

	private void auditSync(String action, E entity) {
		if (isAuditExempt()) return;
		// Hard delete removes the row entirely — nothing meaningful to diff.
		auditLogWriter.logEntitySync(action, entity.getClass().getSimpleName(), extractId(entity), AuditOutcome.SUCCESS, null);
	}

	private boolean isAuditExempt() {
		return this.getClass().isAnnotationPresent(AuditExempt.class);
	}

	private Long extractId(E entity) {
		try {
			Method getId = entity.getClass().getMethod("getId");
			Object id = getId.invoke(entity);
			return id instanceof Long l ? l : null;
		} catch (ReflectiveOperationException ex) {
			return null;
		}
	}

	// A diffing bug must never fail (or roll back) the business operation it's describing.
	private String buildChangedFieldsJson(String action, E entity) {
		try {
			Map<String, Object> newSnapshot = extractDiffableFields(entity);
			Map<String, Object> oldSnapshot;
			if (AuditAction.CREATE.equals(action)) {
				oldSnapshot = Map.of();
			} else {
				oldSnapshot = auditSnapshots.get().remove(entity);
				// No pre-mutation snapshot means we can't safely diff — showing every current
				// value as "changed" would be misleading, so skip rather than guess.
				if (oldSnapshot == null) return null;
			}

			List<Map<String, Object>> changes = new ArrayList<>();
			for (Map.Entry<String, Object> field : newSnapshot.entrySet()) {
				Object oldValue = oldSnapshot.get(field.getKey());
				Object newValue = field.getValue();
				if (oldValue instanceof List<?> || newValue instanceof List<?>) {
					appendCollectionChange(changes, field.getKey(), oldValue, newValue);
				} else if (!Objects.equals(oldValue, newValue)) {
					Map<String, Object> change = new LinkedHashMap<>();
					change.put("field", field.getKey());
					change.put("type", "scalar");
					change.put("oldValue", oldValue);
					change.put("newValue", newValue);
					changes.add(change);
				}
			}
			if (changes.isEmpty()) return null;
			return objectMapper.writeValueAsString(changes);
		} catch (Exception ex) {
			log.warn("Failed to build audit diff for {}: {}", entity.getClass().getSimpleName(), ex.getMessage());
			return null;
		}
	}

	// Git-style: items in both old and new stay "unchanged", items only in the old list are
	// "removed", items only in the new list are "added" — rather than flattening both sides into
	// a single before/after string, which made a partial change to a large set (e.g. adding one
	// permission to a role that already had ten) unreadable.
	private void appendCollectionChange(List<Map<String, Object>> changes, String fieldName, Object oldValue, Object newValue) {
		List<String> oldList = asStringList(oldValue);
		List<String> newList = asStringList(newValue);

		List<String> removed = oldList.stream().filter(v -> !newList.contains(v)).toList();
		List<String> added = newList.stream().filter(v -> !oldList.contains(v)).toList();
		if (removed.isEmpty() && added.isEmpty()) return;

		List<String> unchanged = oldList.stream().filter(newList::contains).toList();
		Map<String, Object> change = new LinkedHashMap<>();
		change.put("field", fieldName);
		change.put("type", "collection");
		change.put("unchanged", unchanged);
		change.put("removed", removed);
		change.put("added", added);
		changes.add(change);
	}

	private List<String> asStringList(Object value) {
		return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of();
	}

	// Only the leaf entity class's own declared fields — inherited BaseEntity/SequenceIdGenerator
	// housekeeping fields (id, createdBy/On, updatedBy/On, deleted) are deliberately excluded, since
	// updatedBy/updatedOn change on every save and would otherwise show up as noise on every diff.
	// Relation fields (a to-one @Entity reference, or a collection of them, e.g. User.roles,
	// Role.permissions) are rendered as their related entities' display names so reassigning
	// roles/permissions/groups shows up in the diff, not just plain scalar column changes.
	private Map<String, Object> extractDiffableFields(E entity) {
		Map<String, Object> values = new LinkedHashMap<>();
		for (Field field : entity.getClass().getDeclaredFields()) {
			if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
			if (field.isAnnotationPresent(AuditIgnore.class) || field.isAnnotationPresent(Transient.class)) continue;

			try {
				field.setAccessible(true);
				Object value = field.get(entity);
				if (isDiffableScalar(field.getType())) {
					values.put(field.getName(), value == null ? null : describeScalar(field, value));
				} else if (isEntityCollection(field)) {
					values.put(field.getName(), describeEntityCollectionAsList(value));
				} else if (field.getType().isAnnotationPresent(Entity.class)) {
					values.put(field.getName(), value == null ? null : describeEntity(value));
				}
			} catch (IllegalAccessException ignored) {}
		}
		return values;
	}

	// Dates are formatted to match the app's own display convention (dd-MM-yyyy [HH:mm:ss]),
	// the same pattern used everywhere a date is rendered in the UI, so the diff table reads
	// naturally instead of showing Java's raw Date#toString(). A @Temporal(DATE) column (e.g.
	// User.expiryDate) only has calendar-date precision in the database, so it's formatted
	// date-only — that also washes out the time-of-day noise a request's Date round-trip
	// (JSON parse, timezone) can introduce, which would otherwise show up as a false "changed"
	// diff for a field the caller never actually touched.
	private String describeScalar(Field field, Object value) {
		if (value instanceof Date date) {
			jakarta.persistence.Temporal temporal = field.getAnnotation(jakarta.persistence.Temporal.class);
			boolean dateOnly = temporal != null && temporal.value() == jakarta.persistence.TemporalType.DATE;
			return new java.text.SimpleDateFormat(dateOnly ? "dd-MM-yyyy" : "dd-MM-yyyy HH:mm:ss").format(date);
		}
		return String.valueOf(value);
	}

	private boolean isDiffableScalar(Class<?> type) {
		return String.class.equals(type)
				|| boolean.class.equals(type) || Boolean.class.equals(type)
				|| int.class.equals(type) || long.class.equals(type)
				|| double.class.equals(type) || float.class.equals(type)
				|| Number.class.isAssignableFrom(type)
				|| Date.class.isAssignableFrom(type)
				|| type.isEnum();
	}

	private boolean isEntityCollection(Field field) {
		if (!Collection.class.isAssignableFrom(field.getType())) return false;
		if (!(field.getGenericType() instanceof ParameterizedType parameterizedType)) return false;
		Type[] typeArgs = parameterizedType.getActualTypeArguments();
		return typeArgs.length == 1 && typeArgs[0] instanceof Class<?> elementType
				&& elementType.isAnnotationPresent(Entity.class);
	}

	private List<String> describeEntityCollectionAsList(Object value) {
		if (!(value instanceof Collection<?> collection)) return List.of();
		return collection.stream()
				.map(this::describeEntity)
				.sorted()
				.toList();
	}

	// Prefers a human-readable name/code over the raw id, since that's what shows up meaningfully
	// in a diff (e.g. "Admin, Recruiter" rather than "#3, #7").
	private String describeEntity(Object entity) {
		for (String getter : new String[] { "getName", "getCode" }) {
			try {
				Object value = entity.getClass().getMethod(getter).invoke(entity);
				if (value != null) return String.valueOf(value);
			} catch (ReflectiveOperationException ignored) {}
		}
		try {
			Object id = entity.getClass().getMethod("getId").invoke(entity);
			return "#" + id;
		} catch (ReflectiveOperationException ex) {
			return String.valueOf(entity);
		}
	}
}
