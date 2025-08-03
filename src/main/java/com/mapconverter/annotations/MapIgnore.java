package com.mapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to exclude fields from map conversion.
 * When applied to a field, that field will be ignored during
 * both toMap() and fromMap() operations.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapIgnore {
}