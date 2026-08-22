package com.bd.erecruitment.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to an entity field to exclude it from the generic old/new value audit diff —
 * secrets and OTP churn should never land in an audit row.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface AuditIgnore {
}
