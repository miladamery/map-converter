package com.mapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields that should be ignored when circular references are detected.
 * This is useful for parent-child relationships where you want to avoid infinite recursion
 * by skipping the parent reference in child objects.
 * 
 * Example:
 * <pre>
 * @MapperGenerate
 * public class Parent {
 *     private List&lt;Child&gt; children;
 * }
 * 
 * @MapperGenerate  
 * public class Child {
 *     @MapIgnoreCircular
 *     private Parent parent; // This will be skipped if circular reference detected
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapIgnoreCircular {
}