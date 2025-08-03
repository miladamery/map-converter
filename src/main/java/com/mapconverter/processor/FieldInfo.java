package com.mapconverter.processor;

import com.mapconverter.enumeration.DateTimeStrategy;

import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;

/**
 * Holds information about a field that needs to be mapped.
 */
public class FieldInfo {
    private final String fieldName;
    private final String mapKey;
    private final TypeMirror fieldType;
    private final boolean ignored;
    private final Element element;
    private final boolean isRecordComponent;
    private final CollectionType collectionType;
    private final TypeMirror elementType;
    private final FieldType fieldType2;
    private final TypeMirror nestedObjectType;
    private final TypeMirror nestedElementType;
    private final boolean isNestedObjectGenerated;
    private final DateTimeStrategy dateTimeStrategy;
    private final String dateTimePattern;
    private final String dateTimeTimezone;
    private final boolean preserveNanos;
    private final boolean lenientParsing;

    public FieldInfo(String fieldName, String mapKey, TypeMirror fieldType, 
                     boolean ignored, Element element, boolean isRecordComponent, CollectionType collectionType, 
                     TypeMirror elementType, FieldType fieldType2, TypeMirror nestedObjectType,
                     TypeMirror nestedElementType, boolean isNestedObjectGenerated,
                     DateTimeStrategy dateTimeStrategy, String dateTimePattern, String dateTimeTimezone,
                     boolean preserveNanos, boolean lenientParsing) {
        this.fieldName = fieldName;
        this.mapKey = mapKey;
        this.fieldType = fieldType;
        this.ignored = ignored;
        this.element = element;
        this.isRecordComponent = isRecordComponent;
        this.collectionType = collectionType;
        this.elementType = elementType;
        this.fieldType2 = fieldType2;
        this.nestedObjectType = nestedObjectType;
        this.nestedElementType = nestedElementType;
        this.isNestedObjectGenerated = isNestedObjectGenerated;
        this.dateTimeStrategy = dateTimeStrategy;
        this.dateTimePattern = dateTimePattern;
        this.dateTimeTimezone = dateTimeTimezone;
        this.preserveNanos = preserveNanos;
        this.lenientParsing = lenientParsing;
    }

    /**
     * Enumeration of field types for enhanced type classification.
     */
    public enum FieldType {
        PRIMITIVE,           // int, String, etc.
        COLLECTION,          // List, Set, Array (existing)
        NESTED_OBJECT,       // Custom class with @MapperGenerate
        NESTED_COLLECTION,   // List<CustomObject>, Set<CustomObject>
        TEMPORAL,            // Date/Time types (java.time.*, java.util.Date, etc.)
        TEMPORAL_COLLECTION, // List<LocalDate>, Set<Instant>, etc.
        ENUM,                // Enum types
        UNKNOWN              // Unsupported types
    }

    public enum CollectionType {
        NONE, LIST, SET, ARRAY
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getMapKey() {
        return mapKey;
    }

    public TypeMirror getFieldType() {
        return fieldType;
    }

    public boolean isIgnored() {
        return ignored;
    }

    public Element getElement() {
        return element;
    }

    public boolean isRecordComponent() {
        return isRecordComponent;
    }

    public CollectionType getCollectionType() {
        return collectionType;
    }

    public TypeMirror getElementType() {
        return elementType;
    }

    public boolean isCollection() {
        return collectionType != CollectionType.NONE;
    }

    public FieldType getFieldType2() {
        return fieldType2;
    }

    public TypeMirror getNestedObjectType() {
        return nestedObjectType;
    }

    public TypeMirror getNestedElementType() {
        return nestedElementType;
    }

    public boolean isNestedObjectGenerated() {
        return isNestedObjectGenerated;
    }

    public boolean isNestedObject() {
        return fieldType2 == FieldType.NESTED_OBJECT;
    }

    public boolean isNestedCollection() {
        return fieldType2 == FieldType.NESTED_COLLECTION;
    }

    public boolean isPrimitive() {
        return fieldType2 == FieldType.PRIMITIVE;
    }

    public boolean isTemporal() {
        return fieldType2 == FieldType.TEMPORAL;
    }
    
    public boolean isTemporalCollection() {
        return fieldType2 == FieldType.TEMPORAL_COLLECTION;
    }
    
    public boolean hasTemporalSupport() {
        return isTemporal() || isTemporalCollection();
    }
    
    public boolean isEnum() {
        return fieldType2 == FieldType.ENUM;
    }
    
    public DateTimeStrategy getDateTimeStrategy() {
        return dateTimeStrategy;
    }
    
    public String getDateTimePattern() {
        return dateTimePattern;
    }
    
    public String getDateTimeTimezone() {
        return dateTimeTimezone;
    }
    
    public boolean isPreserveNanos() {
        return preserveNanos;
    }
    
    public boolean isLenientParsing() {
        return lenientParsing;
    }
}