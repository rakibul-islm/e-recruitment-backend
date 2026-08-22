package com.bd.erecruitment.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to an AbstractBaseService subclass to opt it out of the generic entity-CRUD audit
 * hook, for services whose writes are already covered by explicit, more specific audit calls.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AuditExempt {
}
