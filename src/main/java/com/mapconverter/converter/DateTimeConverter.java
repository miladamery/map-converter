package com.mapconverter.converter;

import com.mapconverter.enumeration.DateTimeStrategy;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Utility class for converting date/time objects to/from map values.
 * 
 * Handles conversion between Java temporal types and their map representations
 * according to the configuration specified in @MapDateTime annotations.
 * 
 * @author Fast Map Converter
 * @since 1.0.0
 */
public class DateTimeConverter {
    
    // Common formatters for performance
    private static final DateTimeFormatter ISO_INSTANT_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final DateTimeFormatter ISO_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter ISO_LOCAL_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter ISO_LOCAL_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;
    
    /**
     * Converts a temporal object to a map value according to the specified configuration.
     * 
     * @param temporal the temporal object to convert
     * @param strategy the conversion strategy
     * @param pattern custom pattern (used with CUSTOM_PATTERN strategy)
     * @param timezone target timezone
     * @param preserveNanos whether to preserve nanosecond precision
     * @param lenientParsing whether to use lenient parsing (not used in toMap)
     * @return the converted map value
     */
    public static Object toMapValue(Object temporal, DateTimeStrategy strategy, String pattern, 
                                  String timezone, boolean preserveNanos, boolean lenientParsing) {
        if (temporal == null) {
            return null;
        }
        
        // Default to AUTO strategy if null
        if (strategy == null) {
            strategy = DateTimeStrategy.AUTO;
        }
        
        // Handle legacy Date types
        if (temporal instanceof Date) {
            return convertLegacyDateToMapValue((Date) temporal, strategy, pattern, timezone, preserveNanos);
        }
        
        // Handle java.time types
        if (temporal instanceof Temporal) {
            return convertTemporalToMapValue((Temporal) temporal, strategy, pattern, timezone, preserveNanos);
        }
        
        throw new IllegalArgumentException("Unsupported temporal type: " + temporal.getClass());
    }
    
    /**
     * Converts a map value to a temporal object of the specified type.
     * 
     * @param value the map value to convert
     * @param targetType the target temporal type
     * @param strategy the conversion strategy
     * @param pattern custom pattern (used with CUSTOM_PATTERN strategy)
     * @param timezone source timezone
     * @param preserveNanos whether to preserve nanosecond precision
     * @param lenientParsing whether to use lenient parsing
     * @return the converted temporal object
     */
    @SuppressWarnings("unchecked")
    public static <T> T fromMapValue(Object value, Class<T> targetType, DateTimeStrategy strategy, 
                                   String pattern, String timezone, boolean preserveNanos, boolean lenientParsing) {
        if (value == null) {
            return null;
        }
        
        // Default to AUTO strategy if null
        if (strategy == null) {
            strategy = DateTimeStrategy.AUTO;
        }
        
        // Handle legacy Date types
        if (Date.class.isAssignableFrom(targetType)) {
            return (T) convertMapValueToLegacyDate(value, targetType, strategy, pattern, timezone, preserveNanos, lenientParsing);
        }
        
        // Handle java.time types
        if (Temporal.class.isAssignableFrom(targetType)) {
            return (T) convertMapValueToTemporal(value, targetType, strategy, pattern, timezone, preserveNanos, lenientParsing);
        }
        
        throw new IllegalArgumentException("Unsupported temporal type: " + targetType);
    }
    
    /**
     * Gets the default conversion strategy for a given temporal type.
     * 
     * @param temporalType the temporal type
     * @return the recommended default strategy
     */
    public static DateTimeStrategy getDefaultStrategy(Class<?> temporalType) {
        if (LocalDate.class.equals(temporalType)) {
            return DateTimeStrategy.ISO_DATE;
        } else if (LocalTime.class.equals(temporalType)) {
            return DateTimeStrategy.ISO_TIME;
        } else if (LocalDateTime.class.equals(temporalType)) {
            return DateTimeStrategy.ISO_LOCAL;
        } else if (Instant.class.equals(temporalType) || 
                   ZonedDateTime.class.equals(temporalType) || 
                   OffsetDateTime.class.equals(temporalType)) {
            return DateTimeStrategy.ISO_INSTANT;
        } else if (Date.class.isAssignableFrom(temporalType)) {
            return DateTimeStrategy.EPOCH_MILLIS;
        }
        return DateTimeStrategy.ISO_INSTANT;
    }
    
    /**
     * Gets the default pattern for a given temporal type and strategy.
     * 
     * @param temporalType the temporal type
     * @param strategy the conversion strategy
     * @return the default pattern string
     */
    public static String getDefaultPattern(Class<?> temporalType, DateTimeStrategy strategy) {
        if (strategy == DateTimeStrategy.CUSTOM_PATTERN) {
            // Provide sensible defaults for custom patterns
            if (LocalDate.class.equals(temporalType)) {
                return "yyyy-MM-dd";
            } else if (LocalTime.class.equals(temporalType)) {
                return "HH:mm:ss";
            } else if (LocalDateTime.class.equals(temporalType)) {
                return "yyyy-MM-dd HH:mm:ss";
            } else if (Date.class.isAssignableFrom(temporalType)) {
                return "yyyy-MM-dd HH:mm:ss";
            }
            return "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
        }
        return "";
    }
    
    // Private helper methods
    
    private static Object convertLegacyDateToMapValue(Date date, DateTimeStrategy strategy, 
                                                    String pattern, String timezone, boolean preserveNanos) {
        switch (strategy) {
            case EPOCH_MILLIS:
                return date.getTime();
            case EPOCH_SECONDS:
                return date.getTime() / 1000L;
            case ISO_INSTANT:
                return date.toInstant().toString();
            case ISO_LOCAL:
                LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), getZoneId(timezone));
                return ldt.format(ISO_LOCAL_DATE_TIME_FORMATTER);
            case ISO_DATE:
                LocalDate ld = date.toInstant().atZone(getZoneId(timezone)).toLocalDate();
                return ld.format(ISO_LOCAL_DATE_FORMATTER);
            case CUSTOM_PATTERN:
                if (pattern.isEmpty()) {
                    throw new IllegalArgumentException("Custom pattern cannot be empty for CUSTOM_PATTERN strategy");
                }
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                return sdf.format(date);
            case LOCALE_DEFAULT:
                return date.toString();
            case AUTO:
                return date.getTime(); // Default to epoch millis for backward compatibility
            default:
                throw new IllegalArgumentException("Unsupported strategy for Date: " + strategy);
        }
    }
    
    private static Object convertTemporalToMapValue(Temporal temporal, DateTimeStrategy strategy, 
                                                  String pattern, String timezone, boolean preserveNanos) {
        if (strategy == DateTimeStrategy.AUTO) {
            strategy = getDefaultStrategy(temporal.getClass());
        }
        
        switch (strategy) {
            case ISO_INSTANT:
                return convertToInstant(temporal, timezone).toString();
            case ISO_LOCAL:
                if (temporal instanceof LocalDateTime) {
                    return ((LocalDateTime) temporal).format(ISO_LOCAL_DATE_TIME_FORMATTER);
                } else {
                    // Convert to LocalDateTime in specified timezone
                    Instant instant = convertToInstant(temporal, timezone);
                    LocalDateTime ldt = LocalDateTime.ofInstant(instant, getZoneId(timezone));
                    return ldt.format(ISO_LOCAL_DATE_TIME_FORMATTER);
                }
            case ISO_DATE:
                if (temporal instanceof LocalDate) {
                    return ((LocalDate) temporal).format(ISO_LOCAL_DATE_FORMATTER);
                } else {
                    // Extract date part
                    Instant instant = convertToInstant(temporal, timezone);
                    LocalDate ld = instant.atZone(getZoneId(timezone)).toLocalDate();
                    return ld.format(ISO_LOCAL_DATE_FORMATTER);
                }
            case ISO_TIME:
                if (temporal instanceof LocalTime) {
                    return ((LocalTime) temporal).format(ISO_LOCAL_TIME_FORMATTER);
                } else {
                    // Extract time part
                    Instant instant = convertToInstant(temporal, timezone);
                    LocalTime lt = instant.atZone(getZoneId(timezone)).toLocalTime();
                    return lt.format(ISO_LOCAL_TIME_FORMATTER);
                }
            case EPOCH_MILLIS:
                Instant instant = convertToInstant(temporal, timezone);
                return instant.toEpochMilli();
            case EPOCH_SECONDS:
                instant = convertToInstant(temporal, timezone);
                return instant.getEpochSecond();
            case CUSTOM_PATTERN:
                if (pattern.isEmpty()) {
                    throw new IllegalArgumentException("Custom pattern cannot be empty for CUSTOM_PATTERN strategy");
                }
                return formatWithCustomPattern(temporal, pattern, timezone);
            case LOCALE_DEFAULT:
                return temporal.toString();
            default:
                throw new IllegalArgumentException("Unsupported strategy: " + strategy);
        }
    }
    
    private static Date convertMapValueToLegacyDate(Object value, Class<?> targetType, DateTimeStrategy strategy, 
                                                  String pattern, String timezone, boolean preserveNanos, boolean lenientParsing) {
        try {
            Instant instant;
            
            if (value instanceof Number) {
                long millis = ((Number) value).longValue();
                if (strategy == DateTimeStrategy.EPOCH_SECONDS) {
                    millis *= 1000;
                }
                instant = Instant.ofEpochMilli(millis);
            } else if (value instanceof String) {
                String str = (String) value;
                instant = parseStringToInstant(str, strategy, pattern, timezone, lenientParsing);
            } else {
                throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to Date");
            }
            
            if (targetType.equals(Date.class)) {
                return Date.from(instant);
            } else if (targetType.equals(Timestamp.class)) {
                return Timestamp.from(instant);
            } else if (targetType.equals(java.sql.Date.class)) {
                return new java.sql.Date(instant.toEpochMilli());
            }
            
            throw new IllegalArgumentException("Unsupported Date type: " + targetType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert value to " + targetType.getSimpleName() + ": " + value, e);
        }
    }
    
    private static Temporal convertMapValueToTemporal(Object value, Class<?> targetType, DateTimeStrategy strategy, 
                                                    String pattern, String timezone, boolean preserveNanos, boolean lenientParsing) {
        try {
            if (strategy == DateTimeStrategy.AUTO) {
                strategy = getDefaultStrategy(targetType);
            }
            
            if (value instanceof Number) {
                return parseNumericToTemporal((Number) value, targetType, strategy, timezone, preserveNanos);
            } else if (value instanceof String) {
                return parseStringToTemporal((String) value, targetType, strategy, pattern, timezone, lenientParsing);
            } else {
                throw new IllegalArgumentException("Cannot convert " + value.getClass() + " to " + targetType.getSimpleName());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert value to " + targetType.getSimpleName() + ": " + value, e);
        }
    }
    
    private static Instant parseStringToInstant(String str, DateTimeStrategy strategy, String pattern, String timezone, boolean lenientParsing) {
        switch (strategy) {
            case ISO_INSTANT:
                return Instant.parse(str);
            case ISO_LOCAL:
                LocalDateTime ldt = LocalDateTime.parse(str, ISO_LOCAL_DATE_TIME_FORMATTER);
                return ldt.atZone(getZoneId(timezone)).toInstant();
            case ISO_DATE:
                LocalDate ld = LocalDate.parse(str, ISO_LOCAL_DATE_FORMATTER);
                return ld.atStartOfDay(getZoneId(timezone)).toInstant();
            case ISO_TIME:
                LocalTime lt = LocalTime.parse(str, ISO_LOCAL_TIME_FORMATTER);
                return lt.atDate(LocalDate.now()).atZone(getZoneId(timezone)).toInstant();
            case CUSTOM_PATTERN:
                return parseCustomPatternToInstant(str, pattern, timezone, lenientParsing);
            default:
                // Try common formats
                try {
                    return Instant.parse(str);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Cannot parse string to Instant: " + str, e);
                }
        }
    }
    
    private static Temporal parseStringToTemporal(String str, Class<?> targetType, DateTimeStrategy strategy, 
                                                String pattern, String timezone, boolean lenientParsing) {
        if (targetType.equals(LocalDate.class)) {
            if (strategy == DateTimeStrategy.ISO_DATE || pattern.isEmpty()) {
                return LocalDate.parse(str, ISO_LOCAL_DATE_FORMATTER);
            } else if (strategy == DateTimeStrategy.CUSTOM_PATTERN) {
                return LocalDate.parse(str, DateTimeFormatter.ofPattern(pattern));
            }
        } else if (targetType.equals(LocalTime.class)) {
            if (strategy == DateTimeStrategy.ISO_TIME || pattern.isEmpty()) {
                return LocalTime.parse(str, ISO_LOCAL_TIME_FORMATTER);
            } else if (strategy == DateTimeStrategy.CUSTOM_PATTERN) {
                return LocalTime.parse(str, DateTimeFormatter.ofPattern(pattern));
            }
        } else if (targetType.equals(LocalDateTime.class)) {
            if (strategy == DateTimeStrategy.ISO_LOCAL || pattern.isEmpty()) {
                return LocalDateTime.parse(str, ISO_LOCAL_DATE_TIME_FORMATTER);
            } else if (strategy == DateTimeStrategy.CUSTOM_PATTERN) {
                return LocalDateTime.parse(str, DateTimeFormatter.ofPattern(pattern));
            }
        } else if (targetType.equals(Instant.class)) {
            return parseStringToInstant(str, strategy, pattern, timezone, lenientParsing);
        } else if (targetType.equals(ZonedDateTime.class)) {
            Instant instant = parseStringToInstant(str, strategy, pattern, timezone, lenientParsing);
            return instant.atZone(getZoneId(timezone));
        } else if (targetType.equals(OffsetDateTime.class)) {
            if (strategy == DateTimeStrategy.ISO_INSTANT) {
                return OffsetDateTime.parse(str);
            } else {
                Instant instant = parseStringToInstant(str, strategy, pattern, timezone, lenientParsing);
                return instant.atZone(getZoneId(timezone)).toOffsetDateTime();
            }
        }
        
        throw new IllegalArgumentException("Unsupported conversion from String to " + targetType.getSimpleName());
    }
    
    private static Temporal parseNumericToTemporal(Number value, Class<?> targetType, DateTimeStrategy strategy, 
                                                 String timezone, boolean preserveNanos) {
        long millis = value.longValue();
        if (strategy == DateTimeStrategy.EPOCH_SECONDS) {
            millis *= 1000;
        }
        
        Instant instant = Instant.ofEpochMilli(millis);
        
        if (targetType.equals(Instant.class)) {
            return instant;
        } else if (targetType.equals(LocalDateTime.class)) {
            return LocalDateTime.ofInstant(instant, getZoneId(timezone));
        } else if (targetType.equals(LocalDate.class)) {
            return instant.atZone(getZoneId(timezone)).toLocalDate();
        } else if (targetType.equals(LocalTime.class)) {
            return instant.atZone(getZoneId(timezone)).toLocalTime();
        } else if (targetType.equals(ZonedDateTime.class)) {
            return instant.atZone(getZoneId(timezone));
        } else if (targetType.equals(OffsetDateTime.class)) {
            return instant.atZone(getZoneId(timezone)).toOffsetDateTime();
        }
        
        throw new IllegalArgumentException("Unsupported conversion from Number to " + targetType.getSimpleName());
    }
    
    private static Instant convertToInstant(Temporal temporal, String timezone) {
        if (temporal instanceof Instant) {
            return (Instant) temporal;
        } else if (temporal instanceof ZonedDateTime) {
            return ((ZonedDateTime) temporal).toInstant();
        } else if (temporal instanceof OffsetDateTime) {
            return ((OffsetDateTime) temporal).toInstant();
        } else if (temporal instanceof LocalDateTime) {
            return ((LocalDateTime) temporal).atZone(getZoneId(timezone)).toInstant();
        } else if (temporal instanceof LocalDate) {
            return ((LocalDate) temporal).atStartOfDay(getZoneId(timezone)).toInstant();
        } else if (temporal instanceof LocalTime) {
            return ((LocalTime) temporal).atDate(LocalDate.now()).atZone(getZoneId(timezone)).toInstant();
        }
        
        throw new IllegalArgumentException("Cannot convert " + temporal.getClass() + " to Instant");
    }
    
    private static String formatWithCustomPattern(Temporal temporal, String pattern, String timezone) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        
        if (temporal instanceof LocalDate || temporal instanceof LocalTime || temporal instanceof LocalDateTime) {
            return formatter.format(temporal);
        } else {
            // Convert to zoned temporal for formatting
            Instant instant = convertToInstant(temporal, timezone);
            ZonedDateTime zdt = instant.atZone(getZoneId(timezone));
            return formatter.format(zdt);
        }
    }
    
    private static Instant parseCustomPatternToInstant(String str, String pattern, String timezone, boolean lenientParsing) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
            
            // Try different parsing approaches
            try {
                return Instant.from(formatter.parse(str));
            } catch (DateTimeParseException e) {
                // Try as LocalDateTime and convert
                LocalDateTime ldt = LocalDateTime.parse(str, formatter);
                return ldt.atZone(getZoneId(timezone)).toInstant();
            }
        } catch (DateTimeParseException e) {
            if (lenientParsing) {
                // Fallback to SimpleDateFormat for legacy compatibility
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                    Date date = sdf.parse(str);
                    return date.toInstant();
                } catch (ParseException pe) {
                    throw new IllegalArgumentException("Cannot parse date string: " + str, pe);
                }
            } else {
                throw new IllegalArgumentException("Cannot parse date string: " + str, e);
            }
        }
    }
    
    private static ZoneId getZoneId(String timezone) {
        if (timezone == null || timezone.isEmpty()) {
            return ZoneId.systemDefault();
        }
        return ZoneId.of(timezone);
    }
}