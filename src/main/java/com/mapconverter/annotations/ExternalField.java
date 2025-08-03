package com.mapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for fields in @ExternalMapper configuration classes that specify
 * how external object fields should be mapped to Map keys.
 * 
 * This annotation provides similar functionality to @MapField but for external objects.
 * It allows custom map key names, field ignoring, and custom converters.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @ExternalMapper(targetClass = User.class)
 * public class UserMappingConfig {
 *     @ExternalField("user_name")     // Custom map key
 *     String username;
 *     
 *     @ExternalField(ignore = true)   // Skip this field
 *     String password;
 *     
 *     String email;                   // Uses field name as map key
 * }
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface ExternalField {
    
    /**
     * Custom map key name for this field.
     * If not specified, uses the field name as the map key.
     * 
     * @return custom map key name
     */
    String value() default "";
    
    /**
     * Custom converter class for this field.
     * The converter must implement ExternalFieldConverter interface.
     * If not specified, uses default conversion logic.
     * 
     * @return converter class for custom field conversion
     */
    Class<?> converter() default Void.class;
    
    /**
     * Whether to ignore this field during mapping.
     * When true, this field will be skipped in both toMap and fromMap operations.
     * 
     * @return true to ignore this field
     */
    boolean ignore() default false;
}