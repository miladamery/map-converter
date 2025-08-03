package com.mapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuring nested object mapping behavior.
 * Provides fine-grained control over how nested objects are processed.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapNested {
    
    /**
     * Maximum depth to traverse for this nested object.
     * Default value of -1 means no depth limit.
     */
    int depth() default -1;
    
    /**
     * Strategy to use for handling circular references.
     */
    CircularRefStrategy strategy() default CircularRefStrategy.REFERENCE_TRACKING;
    
    /**
     * Whether to generate mappers for nested objects automatically.
     * If false, nested objects will be treated as regular objects.
     */
    boolean generateNestedMappers() default true;
    
    /**
     * Enumeration of circular reference handling strategies.
     */
    enum CircularRefStrategy {
        /**
         * Track visited objects to prevent infinite recursion.
         * Returns null when circular reference is detected.
         */
        REFERENCE_TRACKING,
        
        /**
         * Limit maximum depth of nesting.
         * Stops processing when max depth is reached.
         */
        MAX_DEPTH,
        
        /**
         * Store object reference IDs instead of full objects for circular refs.
         * Requires objects to have a getId() method.
         */
        LAZY_REFERENCE
    }
}