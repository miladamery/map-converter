package com.fastmapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark classes for automatic mapper generation.
 * When applied to a class, generates a mapper class with toMap() and fromMap() methods
 * for bidirectional conversion between the annotated class and Map&lt;String, Object&gt;.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface MapperGenerate {
    
    /**
     * Optional custom mapper class name.
     * If not specified, defaults to the annotated class name + "Mapper".
     * 
     * @return the custom mapper class name
     */
    String className() default "";
}