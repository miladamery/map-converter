package com.mapconverter.annotations;

import com.mapconverter.enumeration.DateTimeStrategy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Configures how date/time fields are converted to/from map values.
 * 
 * This annotation provides fine-grained control over temporal type conversion,
 * supporting various output formats, timezone handling, and precision settings.
 * 
 * @author Fast Map Converter
 * @since 1.0.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface MapDateTime {
    
    /**
     * Custom format pattern for date/time conversion.
     * Uses DateTimeFormatter pattern syntax for java.time types
     * and SimpleDateFormat pattern for legacy Date types.
     * 
     * Only used when strategy is CUSTOM_PATTERN.
     * 
     * @return the pattern string, empty for default patterns
     */
    String pattern() default "";
    
    /**
     * Target timezone for conversion.
     * Accepts timezone IDs like "UTC", "America/New_York", "Europe/London".
     * 
     * Only applies to timezone-aware types (ZonedDateTime, OffsetDateTime, Instant).
     * For LocalDateTime, the timezone is used during conversion to/from Instant.
     * 
     * @return the timezone ID, empty for system default
     */
    String timezone() default "";
    
    /**
     * Conversion strategy for date/time values.
     * 
     * @return the strategy to use
     */
    DateTimeStrategy strategy() default DateTimeStrategy.ISO_INSTANT;
    
    /**
     * Whether to preserve nanosecond precision when converting to/from numeric formats.
     * 
     * When true, nanoseconds are preserved in epoch conversions.
     * When false, precision is limited to milliseconds for compatibility.
     * 
     * @return true to preserve nanoseconds, false otherwise
     */
    boolean preserveNanos() default false;
    
    /**
     * Whether to use lenient parsing when converting from string values.
     * 
     * When true, parsing attempts to be flexible with input formats.
     * When false, strict parsing is enforced according to the specified pattern.
     * 
     * @return true for lenient parsing, false for strict
     */
    boolean lenientParsing() default true;
}