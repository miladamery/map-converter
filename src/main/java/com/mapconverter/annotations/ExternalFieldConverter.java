package com.mapconverter.annotations;

/**
 * Interface for custom field converters used with @ExternalField annotation.
 * Allows custom conversion logic for specific field types when mapping
 * external objects to Maps and back.
 * 
 * @param <T> the field type in the external object
 * @param <V> the value type stored in the Map
 * 
 * Example usage:
 * <pre>
 * {@code
 * public class DateToStringConverter implements ExternalFieldConverter<Date, String> {
 *     @Override
 *     public String toMap(Date value) {
 *         return value != null ? value.toString() : null;
 *     }
 *     
 *     @Override  
 *     public Date fromMap(String value) {
 *         return value != null ? Date.valueOf(value) : null;
 *     }
 * }
 * }
 * </pre>
 */
public interface ExternalFieldConverter<T, V> {
    
    /**
     * Converts a field value from the external object to a Map value.
     * 
     * @param value the field value from the external object
     * @return the converted value to store in the Map
     */
    V toMap(T value);
    
    /**
     * Converts a Map value back to the external object field type.
     * 
     * @param value the value from the Map
     * @return the converted value for the external object field
     */
    T fromMap(V value);
}