package com.fastmapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify custom map key names for fields.
 * When applied to a field, the specified value will be used as the map key
 * instead of the field name.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapField {
    
    /**
     * The custom map key name to use for this field.
     * 
     * @return the custom map key name
     */
    String value();
}