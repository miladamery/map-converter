package com.mapconverter.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuration classes that define mapping for external classes
 * that cannot be modified with @MapperGenerate.
 * 
 * This annotation allows mapping third-party classes, library POJOs, DTOs,
 * and other external objects without modifying their source code.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @ExternalMapper(targetClass = User.class, mapperName = "UserMapper")
 * public class UserMappingConfig {
 *     @ExternalField("user_name") 
 *     String username;
 *     
 *     @ExternalField("user_email")
 *     String email;
 *     
 *     @ExternalField(ignore = true)
 *     String password;
 * }
 * }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface ExternalMapper {
    
    /**
     * The external class to generate a mapper for.
     * This class does not need to have @MapperGenerate annotation.
     * 
     * @return the target class to map
     */
    Class<?> targetClass();
    
    /**
     * Optional custom name for the generated mapper class.
     * If not specified, defaults to {TargetClassName}Mapper.
     * 
     * @return custom mapper class name
     */
    String mapperName() default "";
    
    /**
     * Optional package name for the generated mapper.
     * If not specified, uses the same package as the configuration class.
     * 
     * @return package name for generated mapper
     */
    String packageName() default "";
    
    /**
     * Whether to automatically generate mappers for nested external objects.
     * When true, the processor will look for @ExternalMapper configurations
     * for any nested objects found in the target class.
     * 
     * @return true to generate nested mappers automatically
     */
    boolean generateNestedMappers() default true;
}