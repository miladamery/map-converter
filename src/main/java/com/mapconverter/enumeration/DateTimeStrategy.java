package com.mapconverter.enumeration;

/**
 * Defines the conversion strategy for date/time fields when mapping to/from Map values.
 * 
 * Each strategy determines how temporal objects are serialized to map values
 * and how map values are deserialized back to temporal objects.
 * 
 * @author Fast Map Converter
 * @since 1.0.0
 */
public enum DateTimeStrategy {
    
    /**
     * Uses ISO-8601 instant format with UTC timezone.
     * Format: "2023-12-25T10:30:00.123Z"
     * 
     * Suitable for: Instant, ZonedDateTime, OffsetDateTime
     * Output: String in ISO instant format
     */
    ISO_INSTANT,
    
    /**
     * Uses ISO-8601 local date-time format without timezone.
     * Format: "2023-12-25T10:30:00.123"
     * 
     * Suitable for: LocalDateTime, LocalDate, LocalTime
     * Output: String in ISO local format
     */
    ISO_LOCAL,
    
    /**
     * Uses ISO-8601 date format.
     * Format: "2023-12-25"
     * 
     * Suitable for: LocalDate, Date (date only)
     * Output: String in ISO date format
     */
    ISO_DATE,
    
    /**
     * Uses ISO-8601 time format.
     * Format: "10:30:00.123"
     * 
     * Suitable for: LocalTime
     * Output: String in ISO time format
     */
    ISO_TIME,
    
    /**
     * Converts to epoch milliseconds since 1970-01-01T00:00:00Z.
     * Format: 1703505000123
     * 
     * Suitable for: All temporal types
     * Output: Long value representing milliseconds
     */
    EPOCH_MILLIS,
    
    /**
     * Converts to epoch seconds since 1970-01-01T00:00:00Z.
     * Format: 1703505000
     * 
     * Suitable for: All temporal types (loses sub-second precision)
     * Output: Long value representing seconds
     */
    EPOCH_SECONDS,
    
    /**
     * Uses the custom pattern specified in the @MapDateTime annotation.
     * Pattern must be provided via the pattern() attribute.
     * 
     * Suitable for: All temporal types
     * Output: String formatted according to custom pattern
     */
    CUSTOM_PATTERN,
    
    /**
     * Uses the locale-specific default format for the temporal type.
     * Format varies by system locale and temporal type.
     * 
     * Suitable for: All temporal types
     * Output: String in locale-specific format
     */
    LOCALE_DEFAULT,
    
    /**
     * Automatically chooses the most appropriate strategy based on the field type.
     * 
     * Default mappings:
     * - LocalDate → ISO_DATE
     * - LocalTime → ISO_TIME  
     * - LocalDateTime → ISO_LOCAL
     * - Instant, ZonedDateTime, OffsetDateTime → ISO_INSTANT
     * - Date, Timestamp → EPOCH_MILLIS (for backward compatibility)
     * 
     * Suitable for: All temporal types
     * Output: Varies by type
     */
    AUTO
}