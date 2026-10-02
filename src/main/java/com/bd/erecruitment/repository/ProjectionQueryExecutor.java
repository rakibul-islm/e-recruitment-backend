package com.bd.erecruitment.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runs list/page queries that select only the columns a response DTO actually declares, instead of loading whole
 * entities (blobs, lazy collections, long text) and mapping them. A DTO field is selected when the entity has a basic
 * attribute of the same name and type; anything else is left unset for the caller to fill in (see {@link Plan#fullyCovered}).
 */
@Component
public class ProjectionQueryExecutor {

	@PersistenceContext
	private EntityManager em;

	private final Map<String, Plan> plans = new ConcurrentHashMap<>();

	public record Plan(List<Field> fields, List<String> attributes, boolean fullyCovered) {
	}

	/** Which DTO fields can be read straight from columns of {@code entityClass}. */
	public Plan plan(Class<?> entityClass, Class<?> dtoClass) {
		return plans.computeIfAbsent(entityClass.getName() + "->" + dtoClass.getName(), k -> buildPlan(entityClass, dtoClass));
	}

	public <E, R> Page<R> page(Class<E> entityClass, Specification<E> spec, Pageable pageable, Class<R> dtoClass) {
		Plan plan = plan(entityClass, dtoClass);
		CriteriaBuilder cb = em.getCriteriaBuilder();

		CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		Root<E> root = cq.from(entityClass);
		select(cq, root, plan);
		applyWhere(cq, root, cb, spec);
		if (pageable.getSort().isSorted()) cq.orderBy(toOrders(pageable.getSort(), root, cb));

		List<Tuple> rows = em.createQuery(cq)
			.setFirstResult((int) pageable.getOffset())
			.setMaxResults(pageable.getPageSize())
			.getResultList();
		List<R> content = toDtos(rows, plan, dtoClass);

		return PageableExecutionUtils.getPage(content, pageable, () -> count(entityClass, spec));
	}

	public <E, R> List<R> list(Class<E> entityClass, Specification<E> spec, Class<R> dtoClass) {
		Plan plan = plan(entityClass, dtoClass);
		CriteriaBuilder cb = em.getCriteriaBuilder();

		CriteriaQuery<Tuple> cq = cb.createTupleQuery();
		Root<E> root = cq.from(entityClass);
		select(cq, root, plan);
		applyWhere(cq, root, cb, spec);
		return toDtos(em.createQuery(cq).getResultList(), plan, dtoClass);
	}

	private <E> long count(Class<E> entityClass, Specification<E> spec) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<E> root = cq.from(entityClass);
		cq.select(cb.count(root));
		applyWhere(cq, root, cb, spec);
		// A spec may add its own ordering; an ORDER BY on a bare count(...) is invalid on Postgres.
		cq.orderBy(List.of());
		return em.createQuery(cq).getSingleResult();
	}

	private <E> void applyWhere(CriteriaQuery<?> cq, Root<E> root, CriteriaBuilder cb, Specification<E> spec) {
		Predicate predicate = spec == null ? null : spec.toPredicate(root, cq, cb);
		if (predicate != null) cq.where(predicate);
	}

	private <E> void select(CriteriaQuery<Tuple> cq, Root<E> root, Plan plan) {
		List<Selection<?>> selections = new ArrayList<>();
		for (String attribute : plan.attributes()) selections.add(root.get(attribute).alias(attribute));
		cq.multiselect(selections);
	}

	private <E> List<Order> toOrders(Sort sort, Root<E> root, CriteriaBuilder cb) {
		List<Order> orders = new ArrayList<>();
		for (Sort.Order order : sort) {
			Path<?> path = root;
			for (String part : order.getProperty().split("\\.")) path = path.get(part);
			orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
		}
		return orders;
	}

	private <R> List<R> toDtos(List<Tuple> rows, Plan plan, Class<R> dtoClass) {
		List<R> result = new ArrayList<>(rows.size());
		try {
			for (Tuple row : rows) {
				R dto = dtoClass.getDeclaredConstructor().newInstance();
				for (int i = 0; i < plan.fields().size(); i++) {
					Object value = row.get(i);
					Field field = plan.fields().get(i);
					if (value == null && field.getType().isPrimitive()) continue;
					field.set(dto, value);
				}
				result.add(dto);
			}
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot build " + dtoClass.getSimpleName() + " from projection", e);
		}
		return result;
	}

	private Plan buildPlan(Class<?> entityClass, Class<?> dtoClass) {
		EntityType<?> entityType = em.getMetamodel().entity(entityClass);
		List<Field> fields = new ArrayList<>();
		List<String> attributes = new ArrayList<>();
		boolean fullyCovered = true;

		for (Class<?> c = dtoClass; c != null && c != Object.class; c = c.getSuperclass()) {
			for (Field field : c.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
				if (isBasicAttributeOfSameType(entityType, field)) {
					field.setAccessible(true);
					fields.add(field);
					attributes.add(field.getName());
				} else {
					fullyCovered = false;
				}
			}
		}
		return new Plan(List.copyOf(fields), List.copyOf(attributes), fullyCovered);
	}

	private boolean isBasicAttributeOfSameType(EntityType<?> entityType, Field field) {
		try {
			SingularAttribute<?, ?> attribute = entityType.getSingularAttribute(field.getName());
			return attribute.getPersistentAttributeType() == Attribute.PersistentAttributeType.BASIC
				&& box(field.getType()).isAssignableFrom(box(attribute.getJavaType()));
		} catch (IllegalArgumentException notAnAttribute) {
			return false;
		}
	}

	private static Class<?> box(Class<?> type) {
		if (!type.isPrimitive()) return type;
		if (type == boolean.class) return Boolean.class;
		if (type == int.class) return Integer.class;
		if (type == long.class) return Long.class;
		if (type == double.class) return Double.class;
		if (type == float.class) return Float.class;
		if (type == short.class) return Short.class;
		if (type == byte.class) return Byte.class;
		return Character.class;
	}
}
