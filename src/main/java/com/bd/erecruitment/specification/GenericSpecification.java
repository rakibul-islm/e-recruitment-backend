package com.bd.erecruitment.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class GenericSpecification {

    private static final String SUFFIX_GT   = "_gt";
    private static final String SUFFIX_GTE  = "_gte";
    private static final String SUFFIX_LT   = "_lt";
    private static final String SUFFIX_LTE  = "_lte";
    private static final String SUFFIX_IN   = "_in";
    private static final String SUFFIX_LIKE = "_like";
    private static final String SUFFIX_EQ   = "_eq";

    private static final List<String> SUFFIXES = List.of(
            SUFFIX_GTE, SUFFIX_LTE, SUFFIX_GT, SUFFIX_LT, SUFFIX_IN, SUFFIX_LIKE, SUFFIX_EQ
    );

    public static <E> Specification<E> build(Map<String, String> filters) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String key   = entry.getKey();
                String value = entry.getValue();

                if (value == null || value.isBlank()) continue;

                try {
                    String operator = resolveOperator(key);
                    String fieldName = operator.isEmpty() ? key : key.substring(0, key.length() - operator.length());

                    Field field = findField(root.getJavaType(), fieldName);
                    if (field == null) continue;

                    Class<?> type = field.getType();

                    switch (operator) {
                        case SUFFIX_GT ->
                            predicate = cb.and(predicate, cb.gt(root.get(fieldName), (Number) parseNumber(value, type)));
                        case SUFFIX_GTE ->
                            predicate = cb.and(predicate, cb.ge(root.get(fieldName), (Number) parseNumber(value, type)));
                        case SUFFIX_LT ->
                            predicate = cb.and(predicate, cb.lt(root.get(fieldName), (Number) parseNumber(value, type)));
                        case SUFFIX_LTE ->
                            predicate = cb.and(predicate, cb.le(root.get(fieldName), (Number) parseNumber(value, type)));
                        case SUFFIX_IN -> {
                            List<Object> values = Arrays.stream(value.split(","))
                                    .map(v -> isNumeric(type) ? parseNumber(v.trim(), type) : v.trim())
                                    .map(v -> (Object) v)
                                    .toList();
                            predicate = cb.and(predicate, root.get(fieldName).in(values));
                        }
                        case SUFFIX_LIKE ->
                            predicate = cb.and(predicate,
                                    cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%"));
                        case SUFFIX_EQ ->
                            predicate = cb.and(predicate, cb.equal(root.get(fieldName),
                                    isNumeric(type) ? parseNumber(value, type) : value));
                        default -> {
                            if (String.class.equals(type)) {
                                predicate = cb.and(predicate,
                                        cb.like(cb.lower(root.get(fieldName)), "%" + value.toLowerCase() + "%"));
                            } else if (isNumeric(type)) {
                                predicate = cb.and(predicate,
                                        cb.equal(root.get(fieldName), parseNumber(value, type)));
                            } else if (Date.class.isAssignableFrom(type)) {
                                predicate = cb.and(predicate,
                                        cb.equal(root.get(fieldName), parseDate(value)));
                            } else if (Boolean.class.equals(type) || boolean.class.equals(type)) {
                                predicate = cb.and(predicate,
                                        cb.equal(root.get(fieldName), Boolean.parseBoolean(value)));
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            predicate = cb.and(predicate, cb.equal(root.get("deleted"), false));
            query.orderBy(cb.desc(root.get("id")));

            return predicate;
        };
    }

    private static String resolveOperator(String key) {
        return SUFFIXES.stream()
                .filter(key::endsWith)
                .findFirst()
                .orElse("");
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static boolean isNumeric(Class<?> type) {
        return Number.class.isAssignableFrom(type) ||
                int.class.equals(type) || long.class.equals(type) ||
                double.class.equals(type) || float.class.equals(type);
    }

    private static Number parseNumber(String value, Class<?> type) {
        if (Integer.class.equals(type) || int.class.equals(type)) return Integer.parseInt(value);
        if (Long.class.equals(type) || long.class.equals(type)) return Long.parseLong(value);
        if (Double.class.equals(type) || double.class.equals(type)) return Double.parseDouble(value);
        if (Float.class.equals(type) || float.class.equals(type)) return Float.parseFloat(value);
        return Long.parseLong(value);
    }

    private static Date parseDate(String value) throws Exception {
        return new SimpleDateFormat("yyyy-MM-dd").parse(value);
    }
}
